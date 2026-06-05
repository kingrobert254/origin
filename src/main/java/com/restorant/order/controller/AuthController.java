package com.restorant.order.controller;

import com.restorant.order.model.User;
import com.restorant.order.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        // KEMBALI KE SEBELUM HASH: Mencocokkan langsung teks password biasa
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            User user = userOpt.get();
            session.setAttribute("user", user);
            
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin";
            } else if ("CHEF".equals(user.getRole())) {
                return "redirect:/chef";
            } else {
                return "redirect:/user";
            }
        }
        
        model.addAttribute("error", "Username atau Password salah!");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}