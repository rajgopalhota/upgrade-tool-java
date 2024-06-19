package com.example.upgradetool.service;

import com.example.upgradetool.utils.DependencyDataLoader;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class DeprecatedMethodsService {

    @Autowired
    private DependencyDataLoader dataLoader;

    public Map<String, List<Map<String, String>>> findDeprecatedMethodsInZip(InputStream zipInputStream) {
        Map<String, List<Map<String, String>>> deprecatedMethodsMap = new HashMap<>();
        JavaParser javaParser = new JavaParser();

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
                            List<Map<String, String>> deprecatedMethods = new ArrayList<>();
                            cu.accept(new VoidVisitorAdapter<Void>() {
                                @Override
                                public void visit(MethodDeclaration n, Void arg) {
                                    super.visit(n, arg);
                                    if (n.getAnnotationByName("Deprecated").isPresent()) {
                                        String methodName = n.getNameAsString();
                                        String replacement = findReplacementForMethod(methodName);
                                        deprecatedMethods.add(Map.of("methodName", methodName, "replacement", replacement));
                                    }
                                }
                            }, null);
                            deprecatedMethodsMap.put(zipEntry.getName(), deprecatedMethods);
                        }
                    } else {
                        List<String> compilationErrors = handleParseErrors(result, zipEntry.getName());
                        deprecatedMethodsMap.put(zipEntry.getName(), mapErrorsToMap(compilationErrors));
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return deprecatedMethodsMap;
    }

    private String findReplacementForMethod(String methodName) {
        List<Map<String, Object>> methods = dataLoader.getMethods();
        if (methods != null) {
            for (Map<String, Object> method : methods) {
                Object methodNameValue = method.get("methodName");
                if (methodNameValue != null && methodNameValue.equals(methodName)) {
                    return (String) method.get("replacement");
                }
            }
        }
        return "No replacement found";
    }

    private List<String> handleParseErrors(ParseResult<?> result, String fileName) {
        List<String> errors = new ArrayList<>();
        result.getProblems().forEach(problem -> {
            errors.add("Compilation error in " + fileName + ": " + problem.getMessage());
        });
        return errors;
    }

    public List<Map<String, String>> getCompilationErrors(InputStream zipInputStream) {
        List<Map<String, String>> compilationErrors = new ArrayList<>();
        JavaParser javaParser = new JavaParser();

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
                    if (!result.isSuccessful()) {
                        List<String> errors = handleParseErrors(result, zipEntry.getName());
                        compilationErrors.addAll(mapErrorsToMap(errors));
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return compilationErrors;
    }

    private List<Map<String, String>> mapErrorsToMap(List<String> compilationErrors) {
        List<Map<String, String>> errorMaps = new ArrayList<>();
        for (String error : compilationErrors) {
            errorMaps.add(Collections.singletonMap("error", error));
        }
        return errorMaps;
    }
}
