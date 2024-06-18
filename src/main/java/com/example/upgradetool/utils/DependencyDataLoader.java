package com.example.upgradetool.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class DependencyDataLoader {

    private List<Map<String, Object>> dependencies;
    private List<Map<String, Object>> methods;
    private List<Map<String, Object>> apis;

    @PostConstruct
    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream depStream = getClass().getResourceAsStream("/datasets/dependencies.json")) {
            if (depStream == null) {
                throw new IOException("Dependencies dataset not found");
            }
            dependencies = mapper.readValue(depStream, new TypeReference<>() {});
        }

        try (InputStream methodsStream = getClass().getResourceAsStream("/datasets/methods.json")) {
            if (methodsStream == null) {
                throw new IOException("Methods dataset not found");
            }
            methods = mapper.readValue(methodsStream, new TypeReference<>() {});
        }

        try (InputStream apisStream = getClass().getResourceAsStream("/datasets/apis.json")) {
            if (apisStream == null) {
                throw new IOException("APIs dataset not found");
            }
            apis = mapper.readValue(apisStream, new TypeReference<>() {});
        }
    }

    public List<Map<String, Object>> getDependencies() {
        return dependencies;
    }

    public List<Map<String, Object>> getMethods() {
        return methods;
    }

    public List<Map<String, Object>> getApis() {
        return apis;
    }
}