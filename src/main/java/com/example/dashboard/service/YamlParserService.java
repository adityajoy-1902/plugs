package com.example.dashboard.service;

import com.example.dashboard.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.util.*;

@Service
public class YamlParserService {

    private static final Logger logger = LoggerFactory.getLogger(YamlParserService.class);
    private Map<String, String> yamlErrors = new HashMap<>();

    public List<Application> parseYaml() {
        logger.info("Starting YAML parsing process");
        List<Application> applications = new ArrayList<>();
        yamlErrors.clear();

        try {
            logger.debug("Loading YAML configuration file");
            Yaml configYaml = new Yaml(new Constructor(YamlConfig.class, new LoaderOptions()));
            InputStream configInput = new ClassPathResource("yaml-config.yaml").getInputStream();
            YamlConfig yamlConfig = configYaml.load(configInput);

            if (yamlConfig == null) {
                String errorMsg = "YAML configuration is null";
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            if (yamlConfig.getYamlFiles() == null || yamlConfig.getYamlFiles().isEmpty()) {
                String errorMsg = "No YAML files specified in configuration";
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            logger.info("Found {} YAML files to process", yamlConfig.getYamlFiles().size());

            for (String yamlFile : yamlConfig.getYamlFiles()) {
                logger.debug("Processing YAML file: {}", yamlFile);
                List<Application> parsedApps = processYamlFile(yamlFile);
                if (parsedApps != null) {
                    applications.addAll(parsedApps);
                }
            }

            logger.info("Successfully parsed {} valid applications", applications.size());
            return applications;

        } catch (Exception e) {
            String errorMsg = "Error parsing YAML configuration: " + e.getMessage();
            logger.error(errorMsg, e);
            yamlErrors.put("config", errorMsg);
            return new ArrayList<>();
        }
    }

    private List<Application> processYamlFile(String yamlFile) {
        try {
            Resource resource = new ClassPathResource(yamlFile);
            if (!resource.exists()) {
                String errorMsg = "YAML file not found: " + yamlFile;
                logger.error(errorMsg);
                yamlErrors.put(yamlFile, errorMsg);
                return null;
            }

            logger.debug("Loading YAML file: {}", yamlFile);
            LoaderOptions loaderOptions = new LoaderOptions();
            Constructor constructor = new Constructor(ApplicationList.class, loaderOptions);

            TypeDescription applicationListType = new TypeDescription(ApplicationList.class);
            constructor.addTypeDescription(applicationListType);

            Yaml yaml = new Yaml(constructor);
            InputStream inputStream = resource.getInputStream();
            ApplicationList applicationList = yaml.load(inputStream);

            if (applicationList == null) {
                String errorMsg = "Invalid YAML structure - null application list in " + yamlFile;
                logger.error(errorMsg);
                yamlErrors.put(yamlFile, errorMsg);
                return null;
            }

            if (applicationList.getApplications() == null || applicationList.getApplications().isEmpty()) {
                String errorMsg = "No applications found in YAML file: " + yamlFile;
                logger.warn(errorMsg);
                yamlErrors.put(yamlFile, errorMsg);
                return new ArrayList<>();
            }

            logger.debug("Found {} applications in file {}", applicationList.getApplications().size(), yamlFile);
            StringBuilder validationErrors = new StringBuilder();

            for (Application app : applicationList.getApplications()) {
                if (!validateApplication(app, validationErrors)) {
                    logger.warn("Validation errors in file {}: {}", yamlFile, validationErrors);
                }
            }

            logger.info("Successfully processed YAML file: {}", yamlFile);
            return applicationList.getApplications();

        } catch (YAMLException e) {
            String errorMsg = "YAML parsing error in file " + yamlFile + ": " + e.getMessage();
            logger.error(errorMsg, e);
            yamlErrors.put(yamlFile, errorMsg);
            return null;
        } catch (Exception e) {
            String errorMsg = "Error processing YAML file " + yamlFile + ": " + e.getMessage();
            logger.error(errorMsg, e);
            yamlErrors.put(yamlFile, errorMsg);
            return null;
        }
    }

    public Map<String, String> getYamlErrors() {
        return new HashMap<>(yamlErrors);
    }

    private boolean validateApplication(Application app, StringBuilder validationErrors) {
        if (app == null) {
            logger.error("Application is null");
            validationErrors.append("Application is null. ");
            return false;
        }

        if (app.getName() == null || app.getName().trim().isEmpty()) {
            String error = String.format("Application name is missing or empty in application.");
            validationErrors.append(error);
            logger.error("Application validation failed: {}", error);
        }

        if (app.getEnvironments() == null || app.getEnvironments().isEmpty()) {
            String error = String.format("No environments defined for application: %s.", app.getName());
            validationErrors.append(error);
            logger.error("Application validation failed: {}", error);
        } else {
            for (Environment env : app.getEnvironments()) {
                if (!validateEnvironment(env, validationErrors)) {
                    // Continue validating other environments
                }
            }
        }

        return true;
    }

    private boolean validateEnvironment(Environment env, StringBuilder validationErrors) {
        if (env == null) {
            logger.error("Environment is null");
            validationErrors.append("Environment is null. ");
            return false;
        }

        if (env.getName() == null || env.getName().trim().isEmpty()) {
            String error = String.format("Environment name is missing or empty in environment.");
            validationErrors.append(error);
            logger.error("Environment validation failed: {}", error);
        }

        if (env.getServers() == null || env.getServers().isEmpty()) {
            String error = String.format("No servers defined for environment: %s.", env.getName());
            validationErrors.append(error);
            logger.error("Environment validation failed: {}", error);
        } else {
            for (Server server : env.getServers()) {
                if (!validateServer(server, validationErrors)) {
                    // Continue validating other servers
                }
            }
        }

        return true;
    }

    private boolean validateServer(Server server, StringBuilder validationErrors) {
        if (server == null) {
            logger.error("Server is null");
            validationErrors.append("Server is null. ");
            return false;
        }

        if (server.getName() == null || server.getName().trim().isEmpty()) {
            String error = String.format("Server name is missing or empty in server.");
            validationErrors.append(error);
            logger.error("Server validation failed: {}", error);
        }

        if (server.getIp() == null || server.getIp().trim().isEmpty()) {
            String error = String.format("Server IP is missing or empty in server: %s.", server.getName());
            validationErrors.append(error);
            logger.error("Server validation failed: {}", error);
        }

        if (server.getOs() == null || server.getOs().trim().isEmpty()) {
            String error = String.format("Server OS is missing or empty in server: %s.", server.getName());
            validationErrors.append(error);
            logger.error("Server validation failed: {}", error);
        }

        if (server.getServices() == null || server.getServices().isEmpty()) {
            String error = String.format("No services defined for server: %s.", server.getName());
            validationErrors.append(error);
            logger.error("Server validation failed: {}", error);
        } else {
            for (com.example.dashboard.model.Service service : server.getServices()) {
                if (!validateService(service, server.getOs(), validationErrors)) {
                    // Continue validating other services
                }
            }
        }

        return true;
    }

    private boolean validateService(com.example.dashboard.model.Service service, String serverOs, StringBuilder validationErrors) {
        if (service == null) {
            logger.error("Service is null");
            validationErrors.append("Service is null. ");
            return false;
        }

        if (service.getName() == null || service.getName().trim().isEmpty()) {
            String error = String.format("Service name is missing or empty in service.");
            validationErrors.append(error);
            logger.error("Service validation failed: {}", error);
        }

        if (service.getType() == null || service.getType().trim().isEmpty()) {
            String error = String.format("Service type is missing or empty in service: %s.", service.getName());
            validationErrors.append(error);
            logger.error("Service validation failed: {}", error);
        }

        boolean isLinuxServer = "linux".equalsIgnoreCase(serverOs);
        boolean isDbService = "db".equalsIgnoreCase(service.getType());

        if (!isDbService && isLinuxServer) {
            if (service.getStatusCmd() == null || service.getStatusCmd().trim().isEmpty()) {
                String error = String.format("Service status command is missing or empty in service: %s.", service.getName());
                validationErrors.append(error);
                logger.error("Service validation failed: {}", error);
            }

            if (service.getStartupCmd() == null || service.getStartupCmd().trim().isEmpty()) {
                String error = String.format("Service startup command is missing or empty in service: %s.", service.getName());
                validationErrors.append(error);
                logger.error("Service validation failed: {}", error);
            }
        }

        if (isLinuxServer && !isDbService) {
            if (service.getStatusScript() == null || service.getStatusScript().trim().isEmpty()) {
                String error = String.format("Service status script is missing or empty in service: %s (Linux server).", service.getName());
                validationErrors.append(error);
                logger.error("Service validation failed: {}", error);
            }

            if (service.getStartScript() == null || service.getStartScript().trim().isEmpty()) {
                String error = String.format("Service start script is missing or empty in service: %s (Linux server).", service.getName());
                validationErrors.append(error);
                logger.error("Service validation failed: {}", error);
            }
        }

        if ((service.getCmd() == null || service.getCmd().trim().isEmpty()) &&
            (service.getStartupCmd() == null || service.getStartupCmd().trim().isEmpty()) &&
            (service.getStartScript() == null || service.getStartScript().trim().isEmpty()) &&
            (service.getDbType() == null || service.getDbType().trim().isEmpty())) {
            String error = String.format("Service must have at least one command/script in service: %s.", service.getName());
            validationErrors.append(error);
            logger.error("Service validation failed: {}", error);
        }

        if (service.getCmd() != null && service.getCmd().trim().isEmpty()) {
            String error = String.format("Service command is empty in service: %s.", service.getName());
            validationErrors.append(error);
            logger.error("Service validation failed: {}", error);
        }

        if (service.getStartScript() != null && service.getStartScript().trim().isEmpty()) {
            String error = String.format("Service start script is empty in service: %s.", service.getName());
            validationErrors.append(error);
            logger.error("Service validation failed: {}", error);
        }

        if (service.getStatusScript() != null && service.getStatusScript().trim().isEmpty()) {
            String error = String.format("Service status script is empty in service: %s.", service.getName());
            validationErrors.append(error);
            logger.error("Service validation failed: {}", error);
        }

        if (service.getDbType() != null && service.getDbType().trim().isEmpty()) {
            String error = String.format("Service database type is empty in service: %s.", service.getName());
            validationErrors.append(error);
            logger.error("Service validation failed: {}", error);
        }

        if (service.getTnsAlias() != null && service.getTnsAlias().trim().isEmpty()) {
            String error = String.format("Service TNS alias is empty in service: %s.", service.getName());
            validationErrors.append(error);
            logger.error("Service validation failed: {}", error);
        }

        return true;
    }
} 