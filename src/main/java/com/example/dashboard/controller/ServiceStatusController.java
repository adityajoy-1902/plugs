package com.example.dashboard.controller;

import com.example.dashboard.service.ServiceStatusMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ServiceStatusController {

    @Autowired
    private ServiceStatusMonitor monitor;

    @GetMapping("/service-statuses")
    public Map<String, String> getStatuses() {
        return monitor.getAllStatuses();
    }
} 