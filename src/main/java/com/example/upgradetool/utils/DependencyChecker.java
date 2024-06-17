package com.example.upgradetool.utils;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Component
public class DependencyChecker {

    public List<String> getOutdatedDependencies(File buildGradleFile) {
        List<String> outdatedDependencies = new ArrayList<>();
        try {
            String buildGradleContent = Files.readString(buildGradleFile.toPath(), StandardCharsets.UTF_8);

            // Example logic to find outdated dependencies
            if (buildGradleContent.contains("org.springframework.boot:spring-boot-starter-web:")) {
                outdatedDependencies.add("org.springframework.boot:spring-boot-starter-web");
            }
            // Add more logic as per your requirements

        } catch (IOException e) {
            e.printStackTrace();
        }
        return outdatedDependencies;
    }
}
