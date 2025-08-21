package com.example.dashboard.controller;

import com.example.dashboard.model.User;
import com.example.dashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/status")
    @ResponseBody
    public String checkDatabaseConnection() {
        try {
            long userCount = userRepository.count();
            return "Database connection successful! Total users: " + userCount;
        } catch (Exception e) {
            return "Database connection failed: " + e.getMessage();
        }
    }

    @GetMapping("/users")
    @ResponseBody
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/create-user")
    @ResponseBody
    public String createUser(@RequestParam String username, 
                           @RequestParam String password, 
                           @RequestParam String role) {
        try {
            if (userRepository.existsByUsername(username)) {
                return "User already exists: " + username;
            }
            
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setEnabled(true);
            user.setAccountNonExpired(true);
            user.setAccountNonLocked(true);
            user.setCredentialsNonExpired(true);
            
            userRepository.save(user);
            return "User created successfully: " + username;
        } catch (Exception e) {
            return "Error creating user: " + e.getMessage();
        }
    }

    @GetMapping("/test-login")
    public String testLoginPage(Model model) {
        model.addAttribute("message", "Test login page - enter credentials to verify database authentication");
        return "test-login";
    }
}
