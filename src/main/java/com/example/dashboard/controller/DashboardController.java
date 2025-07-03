package com.example.dashboard.controller;

import com.example.dashboard.model.Application;
import com.example.dashboard.model.CommandRequest;
import com.example.dashboard.service.AnsibleExecutionService;
import com.example.dashboard.service.YamlParserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private YamlParserService yamlParserService;

    @Autowired
    private AnsibleExecutionService ansibleExecutionService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Application> applications = yamlParserService.parseYaml();
        model.addAttribute("applications", applications);
        return "dashboard";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username, @RequestParam String password) {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            return "redirect:/dashboard";
        }
        return "redirect:/login?error";
    }

    @PostMapping("/executeCommand")
    @ResponseBody
    public Map<String, String> executeCommand(@RequestBody CommandRequest request) {
        String result = ansibleExecutionService.executeCommand(
            request.getApplication(),
            request.getIp(),
            request.getCommand(),
            request.getOs()
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return response;
    }
} 