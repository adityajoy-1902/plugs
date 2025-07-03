package com.example.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Environment {
    // Required field
    private String name;

    // Required field
    private List<Server> servers;
} 