package com.example.upgradetool.service;

import com.example.upgradetool.utils.DependencyDataLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ApiPackageChecker {

    @Autowired
    private DependencyDataLoader dataLoader;

    public Map<String, List<String>> findDeprecatedApisPackages(InputStream zipInputStream) {
        Map<String, List<String>> deprecatedApisPackagesMap = new HashMap<>();
        JavaParser javaParser = new JavaParser();

        List<Map<String, Object>> apis = dataLoader.getApis(); // Get the APIs dataset

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".java")) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > -1) {
                        byteArrayOutputStream.write(buffer, 0, len);
                    }
                    byteArrayOutputStream.flush();
                    byte[] javaFileBytes = byteArrayOutputStream.toByteArray();

                    ParseResult<CompilationUnit> result = javaParser.parse(new ByteArrayInputStream(javaFileBytes));
                    if (result.isSuccessful()) {
                        CompilationUnit cu = result.getResult().orElse(null);
                        if (cu != null) {
                            List<String> deprecatedApisPackages = new ArrayList<>();
                            cu.accept(new VoidVisitorAdapter<Void>() {
                                @Override
                                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                                    super.visit(n, arg);
                                    String className = n.getNameAsString();
                                    for (Map<String, Object> api : apis) {
                                        if (api.containsKey(className)) {
                                            deprecatedApisPackages.add(className);
                                        }
                                    }
                                }
                            }, null);
                            deprecatedApisPackagesMap.put(zipEntry.getName(), deprecatedApisPackages);
                        }
                    } else {
                        // Handle parse errors if needed
                        System.err.println("Parsing failed for: " + zipEntry.getName());
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return deprecatedApisPackagesMap;
    }
}
