package com.example.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Server {
    // Required field
    private String name;

    // Required field
    private String ip;

    // Required field
    private String os;

    // Required field
    private List<Service> services;
} 