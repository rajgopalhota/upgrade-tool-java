package com.example.upgradetool.controller;

import com.example.upgradetool.service.DeprecatedMethodsService;
import com.example.upgradetool.utils.DependencyChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analyze")
public class FileUploadController {

    @Autowired
    private DependencyChecker dependencyChecker;

    @Autowired
    private DeprecatedMethodsService deprecatedMethodsService;

    @PostMapping("/upload")
    public Map<String, Object> uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                // Save the uploaded file to a temporary location
                Path tempDir = Files.createTempDirectory("uploaded-zip");
                Path tempFilePath = tempDir.resolve(file.getOriginalFilename());
                Files.copy(file.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

                // Analyze the contents of the ZIP file
                List<String> outdatedDependencies = dependencyChecker.getOutdatedDependencies(tempFilePath.toFile());
                Map<String, List<String>> deprecatedMethods = deprecatedMethodsService.findDeprecatedMethodsInZip(Files.newInputStream(tempFilePath));

                // Clean up temporary file
                Files.delete(tempFilePath);

                // Return the analysis report
                return Map.of(
                        "outdatedDependencies", outdatedDependencies,
                        "deprecatedMethods", deprecatedMethods
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Collections.emptyMap();
    }
}
