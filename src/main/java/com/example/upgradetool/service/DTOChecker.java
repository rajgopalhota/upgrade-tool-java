package com.example.upgradetool.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class DTOChecker {

    private final JavaParser javaParser = new JavaParser();
    private static final Logger logger = LoggerFactory.getLogger(DTOChecker.class);

    public List<String> checkForDtosAndSuggestRecords(InputStream zipInputStream) {
        List<String> suggestions = new ArrayList<>();

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
                            cu.accept(new DTOVisitor(), suggestions);
                        }
                    } else {
                        logger.error("Parsing failed for: {}", zipEntry.getName());
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            logger.error("Error processing ZIP file", e);
        }

        return suggestions;
    }

    private static class DTOVisitor extends VoidVisitorAdapter<List<String>> {

        @Override
        public void visit(ClassOrInterfaceDeclaration n, List<String> suggestions) {
            super.visit(n, suggestions);
            if (isDto(n)) {
                suggestions.add("Class " + n.getNameAsString() + " in file " + n.findCompilationUnit().get().getStorage().get().getPath() + " can be converted to a record.");
            }
        }

        private boolean isDto(ClassOrInterfaceDeclaration n) {
            // Check if class is an interface or already a record
            if (n.isInterface() || n.isRecordDeclaration()) {
                return false;
            }

            // Check if class has only private fields
            boolean hasOnlyPrivateFields = n.getFields().stream().allMatch(field -> field.isPrivate());

            // Check if class has only getter and setter methods (excluding constructors)
            boolean hasOnlyGetterAndSetterMethods = n.getMethods().stream()
                    .filter(method -> !method.isConstructorDeclaration())
                    .allMatch(this::isGetterOrSetter);

//            System.out.println(n.getMethods());

            // Check if class has no extends or implements clause
            boolean hasNoExtendsOrImplements = n.getExtendedTypes().isEmpty() && n.getImplementedTypes().isEmpty();

            return hasOnlyPrivateFields && hasOnlyGetterAndSetterMethods && hasNoExtendsOrImplements;
        }

        private boolean isGetterOrSetter(MethodDeclaration method) {
            // Check if method name matches getter or setter pattern
            String methodName = method.getNameAsString();
            System.out.println(methodName);
            boolean isGetterOrSetter = methodName.matches("^(get|is|set)[A-Z].*");


            // Check if method has no parameters and return type is not void
            return isGetterOrSetter &&
                    method.getParameters().isEmpty() &&
                    !method.getType().isVoidType();
        }

    }
}
