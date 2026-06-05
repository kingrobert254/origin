package com.restorant.order.controller;

import com.restorant.order.model.*;
import com.restorant.order.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping
    public String userDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || ! "USER".equals(user.getRole())) return "redirect:/";
        
        // Ambil data keranjang belanja dari session (jika belum ada, buat baru)
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("cart", cart);
        }

        double grandTotal = cart.stream().mapToDouble(CartItem::getSubTotal).sum();

        model.addAttribute("username", user.getUsername());
        model.addAttribute("foods", foodRepository.findAll());
        model.addAttribute("cart", cart);
        model.addAttribute("grandTotal", grandTotal);
        return "user-dashboard";
    }

    // C (Create) - Tambah Item ke Keranjang
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long foodId, @RequestParam int quantity, HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) cart = new ArrayList<>();

        Food food = foodRepository.findById(foodId).orElseThrow(() -> new IllegalArgumentException("Menu tidak ditemukan"));

        // Jika makanan sudah ada di keranjang, tinggal tambahkan quantity-nya
        boolean exists = false;
        for (CartItem item : cart) {
            if (item.getFoodId().equals(foodId)) {
                item.setQuantity(item.getQuantity() + quantity);
                exists = true;
                break;
            }
        }

        if (!exists) {
            cart.add(new CartItem(food.getId(), food.getName(), food.getPrice(), quantity));
        }

        session.setAttribute("cart", cart);
        return "redirect:/user";
    }

    // U (Update) - Ubah Jumlah Item di Keranjang
    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long foodId, @RequestParam int quantity, HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart != null) {
            for (CartItem item : cart) {
                if (item.getFoodId().equals(foodId)) {
                    item.setQuantity(quantity);
                    break;
                }
            }
        }
        return "redirect:/user";
    }

    // D (Delete) - Hapus Item dari Keranjang
    @GetMapping("/cart/delete/{foodId}")
    public String deleteFromCart(@PathVariable Long foodId, HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart != null) {
            cart.removeIf(item -> item.getFoodId().equals(foodId));
        }
        return "redirect:/user";
    }

    // Proses Checkout - Simpan keranjang belanja ke Database (Tabel Master & Detail)
    @PostMapping("/checkout")
    public String checkout(@RequestParam String customerName, HttpSession session) {
        User user = (User) session.getAttribute("user");
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");

        if (user == null || cart == null || cart.isEmpty()) return "redirect:/user";

        // 1. Simpan ke tabel master menggunakan nama pemesan yang diinput manual
        Order order = new Order();
        order.setCustomerName(customerName); // Menampung inputan baru
        order.setStatus("BARU");
        order.setOrderDate(LocalDateTime.now());
        
        double grandTotal = cart.stream().mapToDouble(CartItem::getSubTotal).sum();
        order.setGrandTotal(grandTotal);

        List<OrderDetail> details = new ArrayList<>();
        for (CartItem item : cart) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setFoodName(item.getFoodName());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getPrice());
            detail.setSubTotal(item.getSubTotal());
            details.add(detail);
        }
        order.setDetails(details);

        orderRepository.save(order);
        session.setAttribute("cart", new ArrayList<CartItem>());

        return "redirect:/user?success=true";
    }
}