package com.example.upgradetool.service;

import com.example.upgradetool.utils.DependencyDataLoader;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.*;
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
                    String javaFileContent = new String(javaFileBytes);

                    // Parse the Java file
                    ParseResult<CompilationUnit> result = javaParser.parse(new ByteArrayInputStream(javaFileBytes));
                    if (result.isSuccessful()) {
                        CompilationUnit cu = result.getResult().orElse(null);
                        if (cu != null) {
                            List<String> deprecatedApisPackages = new ArrayList<>();

                            // Check imports
                            for (ImportDeclaration importDecl : cu.getImports()) {
                                String importName = importDecl.getNameAsString();
                                checkAndAddDeprecatedApis(importName, apis, deprecatedApisPackages);
                            }

                            // Check class names and other usages
                            cu.accept(new VoidVisitorAdapter<Void>() {
                                @Override
                                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                                    super.visit(n, arg);
                                    String className = n.getNameAsString();
                                    checkAndAddDeprecatedApis(className, apis, deprecatedApisPackages);
                                }
                            }, null);

                            if (!deprecatedApisPackages.isEmpty()) {
                                deprecatedApisPackagesMap.put(zipEntry.getName(), deprecatedApisPackages);
                            }
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

    private void checkAndAddDeprecatedApis(String name, List<Map<String, Object>> apis, List<String> deprecatedApisPackages) {
        for (Map<String, Object> api : apis) {
            if (name.contains((String) api.get("class"))) {
                String replacement = (String) api.get("replacement");
                deprecatedApisPackages.add(name + " is deprecated. " + replacement);
            }
        }
    }
}
