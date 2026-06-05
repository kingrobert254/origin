package com.restorant.order.controller;

import com.restorant.order.model.Food;
import com.restorant.order.model.Order;
import com.restorant.order.model.User;
import com.restorant.order.repository.FoodRepository;
import com.restorant.order.repository.OrderRepository;
import com.restorant.order.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private UserRepository userRepository;

    // SINKRONISASI JALUR UTAMA UNTUK MAC (Folder Bersama/Shared yang bebas kendala permission)
    private final String UPLOAD_DIR = "/tmp/restoran-uploads/";

    @GetMapping
    public String adminDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) return "redirect:/";

        model.addAttribute("orders", orderRepository.findAll());
        model.addAttribute("foods", foodRepository.findAll());
        model.addAttribute("users", userRepository.findAll()); 
        return "admin-dashboard";
    }

    // ==========================================
    // 1. FITUR TAMBAH USER / KARYAWAN (PLAIN TEXT)
    // ==========================================
    @PostMapping("/user/add")
    public String addUser(@RequestParam String username, 
                          @RequestParam String password, 
                          @RequestParam String role, 
                          HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) return "redirect:/";

        if (userRepository.findByUsername(username).isPresent()) {
            return "redirect:/admin?userError=duplicate";
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password); // Menggunakan plain text
        newUser.setRole(role);

        userRepository.save(newUser); 
        return "redirect:/admin?userSuccess=true";
    }

    // ==========================================
    // 2. FITUR TAMBAH MENU MAKANAN (TERSINKRON)
    // ==========================================
    @PostMapping("/food/add")
    public String addFood(@RequestParam String name, 
                          @RequestParam double price, 
                          @RequestParam("imageFile") MultipartFile imageFile, 
                          HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) return "redirect:/";

        Food food = new Food();
        food.setName(name);
        food.setPrice(price);

        if (!imageFile.isEmpty()) {
            try {
                File dir = new File(UPLOAD_DIR);
                if (!dir.exists()) {
                    dir.mkdirs(); // Otomatis membuat folder di /Users/Shared/ jika belum ada
                }

                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Path path = Paths.get(UPLOAD_DIR + fileName);
                Files.write(path, imageFile.getBytes());

                food.setImageUrl("/uploads/" + fileName);
            } catch (Exception e) {
                e.printStackTrace();
                return "redirect:/admin?menuError=true";
            }
        } else {
            food.setImageUrl("/uploads/default.jpg");
        }

        foodRepository.save(food);
        return "redirect:/admin?menuSuccess=true";
    }

    // ==========================================
    // 3. FITUR TAMPILKAN HALAMAN EDIT MENU
    // ==========================================
    @GetMapping("/food/edit/{id}")
    public String showEditFoodForm(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) return "redirect:/";

        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu tidak ditemukan"));
        
        model.addAttribute("food", food);
        return "admin-edit-food"; 
    }

    // ==========================================
    // 4. FITUR PROSES PERUBAHAN EDIT MENU (TERSINKRON)
    // ==========================================
    @PostMapping("/food/edit")
    public String editFood(@RequestParam Long id,
                           @RequestParam String name, 
                           @RequestParam double price, 
                           @RequestParam("imageFile") MultipartFile imageFile, 
                           HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) return "redirect:/";

        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu tidak ditemukan"));
        
        food.setName(name);
        food.setPrice(price);

        // Jika admin memilih untuk mengganti foto dokumen gambar menu
        if (!imageFile.isEmpty()) {
            try {
                // HAPUS FILE GAMBAR LAMA (Kecuali gambar default agar tidak hilang)
                if (food.getImageUrl() != null && !food.getImageUrl().equals("/uploads/default.jpg")) {
                    String oldFileName = food.getImageUrl().replace("/uploads/", "");
                    File oldFile = new File(UPLOAD_DIR + oldFileName);
                    if (oldFile.exists()) {
                        oldFile.delete(); // Menghapus gambar usang dari folder Shared Mac
                    }
                }

                // SIMPAN FILE GAMBAR BARU
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Path path = Paths.get(UPLOAD_DIR + fileName);
                Files.write(path, imageFile.getBytes());

                food.setImageUrl("/uploads/" + fileName);
            } catch (Exception e) {
                e.printStackTrace();
                return "redirect:/admin?menuError=true";
            }
        }

        foodRepository.save(food);
        return "redirect:/admin?menuUpdateSuccess=true";
    }

    // ==========================================
    // 5. FITUR HAPUS MENU MAKANAN (TERSINKRON)
    // ==========================================
    @GetMapping("/food/delete/{id}")
    public String deleteFood(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) return "redirect:/";

        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu tidak ditemukan"));

        // HAPUS FILE FISIK GAMBAR DARI FOLDER MAC
        if (food.getImageUrl() != null && !food.getImageUrl().equals("/uploads/default.jpg")) {
            String fileName = food.getImageUrl().replace("/uploads/", "");
            File file = new File(UPLOAD_DIR + fileName);
            if (file.exists()) {
                file.delete(); // Menghapus file gambar agar storage laptop tidak penuh
            }
        }

        // HAPUS DATA DARI DATABASE MYSQL
        foodRepository.delete(food);
        return "redirect:/admin?menuDeleteSuccess=true";
    }

    // ==========================================
    // 6. FITUR MANAJEMEN MONITOR ORDERAN AKTIF
    // ==========================================
    @GetMapping("/order/detail/{id}")
    public String viewOrderDetail(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) return "redirect:/";

        Order order = orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Pesanan tidak ditemukan"));
        model.addAttribute("order", order);
        model.addAttribute("details", order.getDetails());
        return "admin-detail";
    }

    @PostMapping("/order/update-status")
    public String updateStatus(@RequestParam Long orderId, @RequestParam String status, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) return "redirect:/";

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));
        
        if ("SELESAI".equals(status)) {
            orderRepository.delete(order); 
        } else {
            order.setStatus(status);
            orderRepository.save(order);
        }

        return "redirect:/admin";
    }
}