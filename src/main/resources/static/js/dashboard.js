document.addEventListener('DOMContentLoaded', function() {
    // Add click event listeners to all restart buttons
    document.querySelectorAll('.restart-btn').forEach(button => {
        button.addEventListener('click', function() {
            // Get server and service information from data attributes
            const serverInfo = {
                appName: this.getAttribute('data-app-name'),
                serverName: this.getAttribute('data-server-name'),
                serverIp: this.getAttribute('data-server-ip'),
                serverOs: this.getAttribute('data-server-os'),
                serviceName: this.getAttribute('data-service-name'),
                serviceType: this.getAttribute('data-service-type'),
                operation: 'restart',
                startupCmd: this.getAttribute('data-startup-cmd'),
                startScript: this.getAttribute('data-start-script'),
                statusCmd: this.getAttribute('data-status-cmd'),
                statusScript: this.getAttribute('data-status-script'),
                stopCmd: this.getAttribute('data-stop-cmd'),
                stopScript: this.getAttribute('data-stop-script')
            };

            // Show loading state
            this.disabled = true;
            this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Restarting...';

            // Send the request
            fetch('/api/service-operation', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(serverInfo)
            })
            .then(response => response.json())
            .then(data => {
                // Show success notification
                showNotification(data.message || 'Service restarted successfully!', 'success');
                
                // Re-enable button
                this.disabled = false;
                this.innerHTML = '<i class="fas fa-sync-alt"></i> Restart';
                
                // Force immediate status update
                setTimeout(() => {
                    updateServiceStatuses();
                }, 2000); // Wait 2 seconds for service to stabilize, then update
            })
            .catch(error => {
                console.error('Error:', error);
                showNotification('Failed to restart service: ' + error.message, 'error');
                
                // Re-enable button
                this.disabled = false;
                this.innerHTML = '<i class="fas fa-sync-alt"></i> Restart';
            });
        });
    });

    // Start polling for service statuses
    updateServiceStatuses();
    setInterval(updateServiceStatuses, 5000); // Poll every 5 seconds for more responsive updates
    
    // Initialize ActivatorService functionality
    initializeActivatorService();
});

function updateServiceStatuses() {
    fetch('/api/service-statuses')
        .then(res => {
            if (!res.ok) {
                throw new Error('Failed to fetch statuses: ' + res.status);
            }
            return res.json();
        })
        .then(statuses => {
            for (const key in statuses) {
                const status = statuses[key];
                const cell = document.querySelector(`[data-status-key="${key}"]`);
                if (cell) {
                    let indicator = cell.querySelector('.status-indicator');
                    if (!indicator) {
                        indicator = document.createElement('span');
                        indicator.className = 'status-indicator';
                        cell.appendChild(indicator);
                    }
                    
                    // Update the indicator color based on status
                    if (status === 'up') {
                        indicator.style.color = 'green';
                        indicator.textContent = '●';
                    } else if (status === 'down') {
                        indicator.style.color = 'red';
                        indicator.textContent = '●';
                    } else {
                        indicator.style.color = 'gray';
                        indicator.textContent = '●';
                    }
                }
            }
        })
        .catch(error => {
            console.error('Error updating service statuses:', error);
            // Don't show notification for polling errors to avoid spam
        });
}

function showNotification(message, type) {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `alert alert-${type === 'success' ? 'success' : 'danger'} alert-dismissible fade show`;
    notification.style.position = 'fixed';
    notification.style.top = '20px';
    notification.style.right = '20px';
    notification.style.zIndex = '9999';
    notification.style.minWidth = '300px';
    
    notification.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    // Add to page
    document.body.appendChild(notification);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 5000);
}

// Add CSS animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Add hover effects for better UX
document.querySelectorAll('.restart-btn').forEach(button => {
    button.addEventListener('mouseenter', function() {
        this.style.transform = 'translateY(-1px)';
    });
    
    button.addEventListener('mouseleave', function() {
        this.style.transform = 'translateY(0)';
    });
});

// Add keyboard shortcuts
document.addEventListener('keydown', function(e) {
    // Ctrl/Cmd + R to refresh the page
    if ((e.ctrlKey || e.metaKey) && e.key === 'r') {
        e.preventDefault();
        window.location.reload();
    }
});

// Add confirmation for restart actions (optional)
function confirmRestart(serverInfo) {
    return confirm(`Are you sure you want to restart the service "${serverInfo.serviceName}" on server "${serverInfo.serverName}" (${serverInfo.serverIp})?`);
}

// Optional: Add this to the restart button click handler for confirmation
// if (!confirmRestart(serverInfo)) {
//     return;
// }

// ActivatorService functionality
function initializeActivatorService() {
    // Load initial activator status
    updateActivatorStatus();
    
    // Set up event listeners
    const triggerManualBtn = document.getElementById('triggerManualBtn');
    const refreshStatusBtn = document.getElementById('refreshStatusBtn');
    const viewLogsBtn = document.getElementById('viewLogsBtn');
    
    if (triggerManualBtn) {
        triggerManualBtn.addEventListener('click', triggerManualRestart);
    }
    
    if (refreshStatusBtn) {
        refreshStatusBtn.addEventListener('click', refreshServiceStatuses);
    }
    
    if (viewLogsBtn) {
        viewLogsBtn.addEventListener('click', toggleLogsView);
    }
    
    // Update activator status every 30 seconds
    setInterval(updateActivatorStatus, 30000);
}

function updateActivatorStatus() {
    fetch('/api/activator/status')
        .then(response => response.json())
        .then(data => {
            // Update next schedule
            const nextScheduleElement = document.getElementById('nextSchedule');
            if (nextScheduleElement) {
                nextScheduleElement.textContent = formatDateTime(data.nextScheduledRestart);
            }
            
            // Update down services count
            const downServicesCountElement = document.getElementById('downServicesCount');
            if (downServicesCountElement) {
                downServicesCountElement.textContent = data.downServicesCount;
                downServicesCountElement.style.color = data.downServicesCount > 0 ? 'red' : 'green';
            }
            
            // Update recent logs count
            const recentLogsCountElement = document.getElementById('recentLogsCount');
            if (recentLogsCountElement) {
                recentLogsCountElement.textContent = data.totalLogs;
            }
            
            // Update logs table if visible
            updateLogsTable(data.recentLogs);
        })
        .catch(error => {
            console.error('Error updating activator status:', error);
        });
}

function triggerManualRestart() {
    const button = document.getElementById('triggerManualBtn');
    if (button) {
        button.disabled = true;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Triggering...';
    }
    
    fetch('/api/activator/trigger-manual', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })
    .then(response => response.json())
    .then(data => {
        showNotification(data.message || 'Manual restart triggered successfully!', 'success');
        
        // Update status after a delay
        setTimeout(() => {
            updateActivatorStatus();
            updateServiceStatuses();
        }, 5000);
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification('Failed to trigger manual restart: ' + error.message, 'error');
    })
    .finally(() => {
        if (button) {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-play"></i> Trigger Manual Restart';
        }
    });
}

function refreshServiceStatuses() {
    const button = document.getElementById('refreshStatusBtn');
    if (button) {
        button.disabled = true;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Refreshing...';
    }
    
    fetch('/api/activator/refresh-status', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            showNotification('Failed to refresh statuses: ' + data.error, 'error');
        } else {
            showNotification(data.message || 'Service statuses refreshed successfully!', 'success');
            // Immediately update both service statuses and activator status
            updateServiceStatuses();
            setTimeout(updateActivatorStatus, 2000);
        }
    })
    .catch(error => {
        console.error('Error refreshing statuses:', error);
        showNotification('Failed to refresh statuses: ' + error.message, 'error');
    })
    .finally(() => {
        if (button) {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-sync-alt"></i> Refresh Status';
        }
    });
}

function toggleLogsView() {
    const logsSection = document.getElementById('activatorLogs');
    const button = document.getElementById('viewLogsBtn');
    
    if (logsSection.style.display === 'none') {
        // Show logs
        fetch('/api/activator/logs')
            .then(response => response.json())
            .then(data => {
                updateLogsTable(data.logs);
                logsSection.style.display = 'block';
                button.innerHTML = '<i class="fas fa-eye-slash"></i> Hide Logs';
            })
            .catch(error => {
                console.error('Error fetching logs:', error);
                showNotification('Failed to fetch logs: ' + error.message, 'error');
            });
    } else {
        // Hide logs
        logsSection.style.display = 'none';
        button.innerHTML = '<i class="fas fa-list"></i> View All Logs';
    }
}

function updateLogsTable(logs) {
    const tableBody = document.getElementById('logsTableBody');
    if (!tableBody) return;
    
    tableBody.innerHTML = '';
    
    logs.forEach(log => {
        const row = document.createElement('tr');
        
        // Extract timestamp if present
        let timestamp = '';
        let message = log;
        
        if (log.startsWith('[') && log.includes(']')) {
            const endBracket = log.indexOf(']');
            timestamp = log.substring(1, endBracket);
            message = log.substring(endBracket + 1).trim();
        }
        
        row.innerHTML = `
            <td>${timestamp || 'N/A'}</td>
            <td>${message}</td>
        `;
        
        tableBody.appendChild(row);
    });
}

function formatDateTime(dateTimeString) {
    if (!dateTimeString) return 'N/A';
    
    try {
        const date = new Date(dateTimeString);
        return date.toLocaleString();
    } catch (error) {
        return dateTimeString;
    }
}