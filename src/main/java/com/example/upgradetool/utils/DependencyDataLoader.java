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

    @PostConstruct
    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getResourceAsStream("/dependency-dataset.json")) {
            if (inputStream == null) {
                throw new IOException("Dependency dataset not found");
            }
            Map<String, List<Map<String, Object>>> data = mapper.readValue(inputStream, new TypeReference<>() {});
            dependencies = data.get("dependencies");
            methods = data.get("methods");
        }
    }

    public List<Map<String, Object>> getDependencies() {
        return dependencies;
    }

    public List<Map<String, Object>> getMethods() {
        return methods;
    }
}
