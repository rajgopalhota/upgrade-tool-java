package com.example.upgradetool.service;

import com.example.upgradetool.utils.DependencyDataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class DependencyChecker {

    @Autowired
    private DependencyDataLoader dataLoader;

    public List<String> getOutdatedDependencies(File inputFile) throws IOException {
        List<String> outdatedDependencies = new ArrayList<>();

        if (inputFile.getName().toLowerCase().endsWith(".zip")) {
            try (ZipFile zipFile = new ZipFile(inputFile)) {
                zipFile.stream()
                        .filter(entry -> entry.getName().endsWith(".gradle") || entry.getName().endsWith(".xml"))
                        .forEach(entry -> {
                            try {
                                List<String> dependencies = readDependenciesFromEntry(zipFile, entry);
                                outdatedDependencies.addAll(dependencies);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
        } else if (inputFile.getName().toLowerCase().endsWith(".gradle")) {
            outdatedDependencies.addAll(getOutdatedDependenciesFromGradle(inputFile));
        } else if (inputFile.getName().toLowerCase().endsWith(".xml")) {
            outdatedDependencies.addAll(getOutdatedDependenciesFromMaven(inputFile));
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + inputFile.getName());
        }

        return outdatedDependencies;
    }

    private List<String> readDependenciesFromEntry(ZipFile zipFile, ZipEntry entry) throws IOException {
        List<String> outdatedDependencies = new ArrayList<>();
        try {
            String content = new String(zipFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            if (entry.getName().endsWith(".gradle")) {
                outdatedDependencies.addAll(getOutdatedDependenciesFromGradleContent(content));
            } else if (entry.getName().endsWith(".xml")) {
                outdatedDependencies.addAll(getOutdatedDependenciesFromMavenContent(content));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outdatedDependencies;
    }

    private List<String> getOutdatedDependenciesFromGradle(File buildFile) throws IOException {
        String content = Files.readString(buildFile.toPath(), StandardCharsets.ISO_8859_1);
        return getOutdatedDependenciesFromGradleContent(content);
    }

    private List<String> getOutdatedDependenciesFromGradleContent(String content) {
        List<String> outdatedDependencies = new ArrayList<>();
        List<Map<String, Object>> dependencyData = dataLoader.getDependencies();

        // Regex to match dependencies in build.gradle (with or without quotes)
        Pattern pattern = Pattern.compile("implementation\\s*['\"]?([^:]+):([^:]+):([^'\"]+)['\"]?");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String groupId = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);
            findAndAddOutdatedDependency(outdatedDependencies, dependencyData, groupId, artifactId, version);
        }

        return outdatedDependencies;
    }

    private List<String> getOutdatedDependenciesFromMaven(File pomFile) throws IOException {
        String content = Files.readString(pomFile.toPath(), StandardCharsets.ISO_8859_1);
        return getOutdatedDependenciesFromMavenContent(content);
    }

    private List<String> getOutdatedDependenciesFromMavenContent(String content) {
        List<String> outdatedDependencies = new ArrayList<>();
        List<Map<String, Object>> dependencyData = dataLoader.getDependencies();

        // Regex to match dependencies in pom.xml
        Pattern pattern = Pattern.compile("<dependency>.*?<groupId>(.*?)</groupId>.*?<artifactId>(.*?)</artifactId>.*?<version>(.*?)</version>.*?</dependency>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String groupId = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);

            findAndAddOutdatedDependency(outdatedDependencies, dependencyData, groupId, artifactId, version);
        }

        return outdatedDependencies;
    }

    private void findAndAddOutdatedDependency(List<String> outdatedDependencies, List<Map<String, Object>> dependencyData, String groupId, String artifactId, String version) {
        for (Map<String, Object> dependency : dependencyData) {
            if (dependency.get("groupId").equals(groupId) && dependency.get("artifactId").equals(artifactId)) {
                String minVersion = (String) dependency.get("minVersion");
                boolean deprecatedInJava17 = (Boolean) dependency.get("deprecatedInJava17");
                if (version.compareTo(minVersion) < 0) {
                    String message = groupId + ":" + artifactId + ":" + version + " (Outdated, minVersion: " + minVersion + ")";
                    outdatedDependencies.add(message);
                } else if (deprecatedInJava17) {
                    String message = groupId + ":" + artifactId + ":" + version + " (Deprecated in Java 17)";
                    outdatedDependencies.add(message);
                }
                break;
            }
        }
    }
}
