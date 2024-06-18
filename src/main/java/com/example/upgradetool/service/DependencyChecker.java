package com.example.upgradetool.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DependencyChecker {

    @Autowired
    private DependencyDataLoader dataLoader;

    public List<String> getOutdatedDependencies(File buildFile) throws IOException {

        List<String> outdatedDependencies = new ArrayList<>();
        String content = Files.readString(buildFile.toPath(), StandardCharsets.ISO_8859_1);
        // Regex to match dependencies in build.gradle
        Pattern pattern = Pattern.compile("implementation\\s*'([^:]+):([^:]+):([^']+)'");
        Matcher matcher = pattern.matcher(content);

        List<Map<String, Object>> dependencyData = dataLoader.getDependencies();

        while (matcher.find()) {
            String groupId = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);

            for (Map<String, Object> dependency : dependencyData) {
                if (dependency.get("groupId").equals(groupId) && dependency.get("artifactId").equals(artifactId)) {
                    String minVersion = (String) dependency.get("minVersion");
                    if (version.compareTo(minVersion) < 0) {
                        outdatedDependencies.add(groupId + ":" + artifactId + ":" + version);
                    }
                    break;
                }
            }
        }

        return outdatedDependencies;
    }
}