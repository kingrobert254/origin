package com.restorant.order.controller;

import com.restorant.order.model.Order;
import com.restorant.order.model.User;
import com.restorant.order.repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/chef")
public class ChefController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping
    public String chefDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        // Validasi agar hanya role CHEF yang bisa masuk
        if (user == null || !"CHEF".equals(user.getRole())) return "redirect:/";

        // Chef hanya perlu melihat pesanan yang statusnya "BARU" atau "SEDANG DIMASAK"
        model.addAttribute("orders", orderRepository.findAll());
        return "chef-dashboard";
    }

    // FITUR BARU: Chef mengubah status menjadi Selesai Dimasak / Sedang Dimasak
    @PostMapping("/order/update-status")
    public String chefUpdateStatus(@RequestParam Long orderId, @RequestParam String status, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"CHEF".equals(user.getRole())) return "redirect:/";

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));
        order.setStatus(status);
        orderRepository.save(order);

        return "redirect:/chef";
    }
}