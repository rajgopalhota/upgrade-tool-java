package com.example.upgradetool.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
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
public class DeprecatedMethodsService {

    public Map<String, List<String>> findDeprecatedMethodsInZip(InputStream zipInputStream) {
        Map<String, List<String>> deprecatedMethodsMap = new HashMap<>();
        JavaParser javaParser = new JavaParser(); // Create an instance of JavaParser

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".java")) {
                    // Read the content of the .java file
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > -1) {
                        byteArrayOutputStream.write(buffer, 0, len);
                    }
                    byteArrayOutputStream.flush();
                    byte[] javaFileBytes = byteArrayOutputStream.toByteArray();

                    // Parse the Java file
                    ParseResult<CompilationUnit> result = javaParser.parse(new ByteArrayInputStream(javaFileBytes));
                    if (result.isSuccessful()) {
                        CompilationUnit cu = result.getResult().orElse(null); // Get the CompilationUnit
                        if (cu != null) {
                            List<String> deprecatedMethods = new ArrayList<>();
                            cu.accept(new VoidVisitorAdapter<Void>() {
                                @Override
                                public void visit(com.github.javaparser.ast.body.MethodDeclaration n, Void arg) {
                                    super.visit(n, arg);
                                    if (n.getAnnotationByName("Deprecated").isPresent()) {
                                        deprecatedMethods.add(n.getNameAsString());
                                    }
                                }
                            }, null);
                            deprecatedMethodsMap.put(zipEntry.getName(), deprecatedMethods);
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
        return deprecatedMethodsMap;
    }
}
