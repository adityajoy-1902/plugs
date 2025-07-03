package com.example.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    // Required field
    private String name;

    // Required field
    private String type;

    // At least one of these should be present
    private String cmd;
    private String startupCmd;
    private String statusCmd;
    private String startScript;
    private String statusScript;
    private String dbType;
    private String tnsAlias;
    private String stopCmd;
    private String stopScript;

    public String getStatusCmd() {
        return statusCmd;
    }
    public void setStatusCmd(String statusCmd) {
        this.statusCmd = statusCmd;
    }
    public String getStatusScript() {
        return statusScript;
    }
    public void setStatusScript(String statusScript) {
        this.statusScript = statusScript;
    }
    public String getStartupCmd() {
        return startupCmd;
    }
    public void setStartupCmd(String startupCmd) {
        this.startupCmd = startupCmd;
    }
    public String getStartScript() {
        return startScript;
    }
    public void setStartScript(String startScript) {
        this.startScript = startScript;
    }
    public String getStopCmd() {
        return stopCmd;
    }
    public void setStopCmd(String stopCmd) {
        this.stopCmd = stopCmd;
    }
    public String getStopScript() {
        return stopScript;
    }
    public void setStopScript(String stopScript) {
        this.stopScript = stopScript;
    }
} 