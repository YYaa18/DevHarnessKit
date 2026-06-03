package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphParseResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaLiteParser implements GraphSourceParser {
    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([A-Za-z_][\\w.]*)\\s*;");
    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([A-Za-z_][\\w.]*\\*?)\\s*;");
    private static final Pattern TYPE = Pattern.compile("\\b(class|interface|enum)\\s+([A-Za-z_][\\w]*)\\b([^\\{;]*)");
    private static final Pattern METHOD = Pattern.compile("^\\s*((?:public|protected|private)\\s+)?"
            + "(?:static\\s+)?(?:final\\s+)?[A-Za-z_][\\w<>\\[\\], ?\\.]*\\s+"
            + "([A-Za-z_][\\w]*)\\s*\\(([^;{}]*)\\)\\s*(?:throws\\s+[^\\{]+)?\\{?\\s*$");
    private static final Pattern CONSTRUCTOR = Pattern.compile("^\\s*((?:public|protected|private)\\s+)?"
            + "([A-Z][A-Za-z0-9_]*)\\s*\\(([^;{}]*)\\)\\s*\\{?\\s*$");
    private static final Pattern METHOD_CALL = Pattern.compile("\\b([A-Za-z_][\\w]*)\\.([A-Za-z_][\\w]*)\\s*\\(");
    private static final Pattern NEW_TYPE = Pattern.compile("\\bnew\\s+([A-Z][A-Za-z0-9_]*)\\s*\\(");
    private static final Pattern WORD = Pattern.compile("\\b([A-Z][A-Za-z0-9_]*)\\b");
    private static final Pattern VARIABLE_DECLARATION = Pattern.compile("\\b([A-Z][A-Za-z0-9_]*)\\s+([a-z][A-Za-z0-9_]*)\\b\\s*(?:=|;|,|\\))");

    public boolean supports(GraphFileEntry entry) {
        return "java".equals(entry.language());
    }

    public void parse(Path projectRoot, GraphFileEntry entry, GraphParseResult.Builder builder) throws Exception {
        Path file = projectRoot.resolve(entry.relativePath());
        List<String> lines = Files.readAllLines(file);
        String packageName = "";
        List<String> imports = new ArrayList<String>();
        Map<String, String> importBySimpleName = new LinkedHashMap<String, String>();
        String currentTypeKey = "";
        String currentTypeQualifiedName = "";
        String currentTypeName = "";
        String currentMethodKey = "";
        Map<String, String> variableTypes = new LinkedHashMap<String, String>();
        List<String> pendingAnnotations = new ArrayList<String>();

        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            String line = lines.get(index);
            String trimmed = line.trim();
            String code = stripLineComment(line);

            Matcher packageMatcher = PACKAGE.matcher(code);
            if (packageMatcher.find()) {
                packageName = packageMatcher.group(1);
                addPackageNode(builder, entry, packageName, lineNumber);
                continue;
            }

            Matcher importMatcher = IMPORT.matcher(code);
            if (importMatcher.find()) {
                String imported = importMatcher.group(1);
                imports.add(imported);
                importBySimpleName.put(simpleName(imported), imported);
                continue;
            }

            if (trimmed.startsWith("@")) {
                pendingAnnotations.add(trimmed);
                continue;
            }

            Matcher typeMatcher = TYPE.matcher(code);
            if (typeMatcher.find()) {
                String typeKind = typeMatcher.group(1);
                currentTypeName = typeMatcher.group(2);
                currentTypeQualifiedName = packageName.length() == 0
                        ? currentTypeName : packageName + "." + currentTypeName;
                currentTypeKey = javaTypeKey(currentTypeQualifiedName);
                builder.addNode(new GraphNode(currentTypeKey, typeKind, currentTypeName,
                        currentTypeQualifiedName, entry.relativePath(), lineNumber, lineNumber,
                        "java", visibility(code), signature(code), 90, "lite",
                        "java " + typeKind + " declaration"));
                if (packageName.length() > 0) {
                    builder.addEdge(new GraphEdge("contains", packageKey(packageName), currentTypeKey,
                            entry.relativePath(), 90, "lite", "package contains type"));
                }
                addImportEdges(builder, entry, currentTypeKey, imports, lineNumber);
                addTypeInheritanceEdges(builder, entry, currentTypeKey, typeMatcher.group(3), importBySimpleName,
                        packageName, lineNumber);
                pendingAnnotations.clear();
                continue;
            }

            if (currentTypeKey.length() > 0) {
                MethodMatch method = matchMethod(code, currentTypeName);
                if (method != null) {
                    addParameterTypes(variableTypes, method.parameters, importBySimpleName, packageName);
                    String qualifiedName = currentTypeQualifiedName + "#" + method.name;
                    String methodKey = javaMethodKey(qualifiedName);
                    String nodeKind = hasAnnotation(pendingAnnotations, "Test") ? "test_case" : "method";
                    builder.addNode(new GraphNode(methodKey, nodeKind, method.name, qualifiedName,
                            entry.relativePath(), lineNumber, lineNumber, "java", method.visibility,
                            signature(code), 85, "lite", "java method signature"));
                    builder.addEdge(new GraphEdge("contains", currentTypeKey, methodKey, entry.relativePath(),
                            90, "lite", "type contains method"));
                    addAnnotationAndRouteNodes(builder, entry, methodKey, pendingAnnotations, lineNumber);
                    pendingAnnotations.clear();
                    currentMethodKey = methodKey;
                }
                addVariableTypes(variableTypes, code, importBySimpleName, packageName);
                if (currentMethodKey.length() > 0) {
                    addCallEdges(builder, entry, currentMethodKey, code, importBySimpleName, variableTypes,
                            packageName, lineNumber);
                }
            }
        }
    }

    private void addPackageNode(GraphParseResult.Builder builder, GraphFileEntry entry,
                                String packageName, int lineNumber) {
        builder.addNode(new GraphNode(packageKey(packageName), "package", packageName, packageName,
                entry.relativePath(), lineNumber, lineNumber, "java", "", "", 90, "lite",
                "java package declaration"));
    }

    private void addImportEdges(GraphParseResult.Builder builder, GraphFileEntry entry, String typeKey,
                                List<String> imports, int lineNumber) {
        for (String imported : imports) {
            String importKey = "java_import:" + imported;
            builder.addNode(new GraphNode(importKey, "import", simpleName(imported), imported,
                    entry.relativePath(), lineNumber, lineNumber, "java", "", "", 80, "lite",
                    "java import declaration"));
            builder.addEdge(new GraphEdge("imports", typeKey, importKey, entry.relativePath(), 80,
                    "lite", "type imports symbol"));
        }
    }

    private void addTypeInheritanceEdges(GraphParseResult.Builder builder, GraphFileEntry entry, String typeKey,
                                         String tail, Map<String, String> imports, String packageName,
                                         int lineNumber) {
        addKeywordTargets(builder, entry, typeKey, tail, imports, packageName, lineNumber, "extends");
        addKeywordTargets(builder, entry, typeKey, tail, imports, packageName, lineNumber, "implements");
    }

    private void addKeywordTargets(GraphParseResult.Builder builder, GraphFileEntry entry, String typeKey,
                                   String tail, Map<String, String> imports, String packageName, int lineNumber,
                                   String keyword) {
        int offset = tail == null ? -1 : tail.indexOf(keyword);
        if (offset < 0) {
            return;
        }
        String targets = tail.substring(offset + keyword.length()).replace('{', ' ').trim();
        int nextKeyword = "extends".equals(keyword) ? targets.indexOf("implements") : -1;
        if (nextKeyword >= 0) {
            targets = targets.substring(0, nextKeyword);
        }
        for (String raw : targets.split(",")) {
            String name = raw.trim().replaceAll("<.*>", "").trim();
            if (name.length() == 0) {
                continue;
            }
            String qualified = resolveType(name, imports, packageName);
            String targetKey = javaTypeKey(qualified);
            builder.addNode(new GraphNode(targetKey, "type_reference", simpleName(qualified), qualified,
                    entry.relativePath(), lineNumber, lineNumber, "java", "", "", 70, "lite",
                    keyword + " target"));
            builder.addEdge(new GraphEdge(keyword, typeKey, targetKey, entry.relativePath(), 70,
                    "lite", keyword + " declaration"));
        }
    }

    private void addAnnotationAndRouteNodes(GraphParseResult.Builder builder, GraphFileEntry entry, String methodKey,
                                            List<String> annotations, int lineNumber) {
        for (String annotation : annotations) {
            String name = annotationName(annotation);
            if (name.length() == 0) {
                continue;
            }
            String annotationKey = "java_annotation:" + name;
            builder.addNode(new GraphNode(annotationKey, "annotation", name, name, entry.relativePath(),
                    lineNumber, lineNumber, "java", "", "", 75, "lite", "method annotation"));
            builder.addEdge(new GraphEdge("references", methodKey, annotationKey, entry.relativePath(),
                    75, "lite", "method annotation"));
            Route route = route(annotation, name);
            if (route != null) {
                String routeKey = "route:" + route.method + ":" + route.path;
                builder.addNode(new GraphNode(routeKey, "route", route.method + " " + route.path,
                        route.method + " " + route.path, entry.relativePath(), lineNumber, lineNumber,
                        "java", "", "", 80, "lite", "route annotation"));
                builder.addEdge(new GraphEdge("handles_route", methodKey, routeKey, entry.relativePath(),
                        80, "lite", "method route annotation"));
            }
        }
    }

    private void addCallEdges(GraphParseResult.Builder builder, GraphFileEntry entry, String methodKey,
                              String code, Map<String, String> imports, Map<String, String> variableTypes,
                              String packageName, int lineNumber) {
        Matcher callMatcher = METHOD_CALL.matcher(code);
        while (callMatcher.find()) {
            String receiver = callMatcher.group(1);
            String method = callMatcher.group(2);
            if (isIgnoredReceiver(receiver)) {
                continue;
            }
            String receiverType = variableTypes.get(receiver);
            if (receiverType != null && receiverType.length() > 0) {
                String qualified = receiverType + "#" + method;
                String targetKey = javaMethodKey(qualified);
                builder.addNode(new GraphNode(targetKey, "method_reference", method, qualified,
                        entry.relativePath(), lineNumber, lineNumber, "java", "", "", 70, "lite",
                        "typed method call expression"));
                builder.addEdge(new GraphEdge("calls", methodKey, targetKey, entry.relativePath(), 75,
                        "lite", "call " + qualified + "()"));
            } else if (isTypeReceiver(receiver)) {
                String qualifiedType = resolveType(receiver, imports, packageName);
                String qualified = qualifiedType + "#" + method;
                String targetKey = javaMethodKey(qualified);
                builder.addNode(new GraphNode(targetKey, "method_reference", method, qualified,
                        entry.relativePath(), lineNumber, lineNumber, "java", "", "", 70, "lite",
                        "static method call expression"));
                builder.addEdge(new GraphEdge("calls", methodKey, targetKey, entry.relativePath(), 75,
                        "lite", "call " + qualified + "()"));
            } else {
                String qualified = receiver + "." + method;
                String targetKey = "java_call:" + qualified;
                builder.addNode(new GraphNode(targetKey, "reference", method, qualified, entry.relativePath(),
                        lineNumber, lineNumber, "java", "", "", 65, "lite", "method call expression"));
                builder.addEdge(new GraphEdge("calls", methodKey, targetKey, entry.relativePath(), 65,
                        "lite", "call " + qualified + "()"));
            }
        }
        Matcher newMatcher = NEW_TYPE.matcher(code);
        while (newMatcher.find()) {
            String type = newMatcher.group(1);
            String qualified = imports.containsKey(type) ? imports.get(type) : type;
            String targetKey = javaTypeKey(qualified);
            builder.addNode(new GraphNode(targetKey, "type_reference", type, qualified, entry.relativePath(),
                    lineNumber, lineNumber, "java", "", "", 65, "lite", "constructor reference"));
            builder.addEdge(new GraphEdge("references", methodKey, targetKey, entry.relativePath(), 65,
                    "lite", "new " + type + "()"));
        }
        Matcher wordMatcher = WORD.matcher(code);
        while (wordMatcher.find()) {
            String simple = wordMatcher.group(1);
            if (imports.containsKey(simple)) {
                String qualified = imports.get(simple);
                String targetKey = javaTypeKey(qualified);
                builder.addNode(new GraphNode(targetKey, "type_reference", simple, qualified,
                        entry.relativePath(), lineNumber, lineNumber, "java", "", "", 60, "lite",
                        "imported type reference"));
                builder.addEdge(new GraphEdge("references", methodKey, targetKey, entry.relativePath(),
                        60, "lite", "imported type reference"));
            }
        }
    }

    private MethodMatch matchMethod(String code, String currentTypeName) {
        if (isControlStatement(code) || code.contains(" class ") || code.contains(" interface ")) {
            return null;
        }
        Matcher constructor = CONSTRUCTOR.matcher(code);
        if (constructor.find() && constructor.group(2).equals(currentTypeName)) {
            return new MethodMatch(currentTypeName, visibility(code), constructor.group(3));
        }
        Matcher method = METHOD.matcher(code);
        if (method.find()) {
            String name = method.group(2);
            if (!isControlKeyword(name)) {
                return new MethodMatch(name, visibility(code), method.group(3));
            }
        }
        return null;
    }

    private void addParameterTypes(Map<String, String> variableTypes, String parameters,
                                   Map<String, String> imports, String packageName) {
        if (parameters == null || parameters.trim().length() == 0) {
            return;
        }
        String[] parts = parameters.split(",");
        for (String part : parts) {
            Matcher matcher = VARIABLE_DECLARATION.matcher(part.trim() + ";");
            if (matcher.find()) {
                variableTypes.put(matcher.group(2), resolveType(matcher.group(1), imports, packageName));
            }
        }
    }

    private void addVariableTypes(Map<String, String> variableTypes, String code,
                                  Map<String, String> imports, String packageName) {
        Matcher matcher = VARIABLE_DECLARATION.matcher(code);
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            if (!isControlKeyword(name)) {
                variableTypes.put(name, resolveType(type, imports, packageName));
            }
        }
    }

    private String resolveType(String type, Map<String, String> imports, String packageName) {
        if (type == null || type.length() == 0) {
            return "";
        }
        if (imports.containsKey(type)) {
            return imports.get(type);
        }
        if (type.indexOf('.') >= 0) {
            return type;
        }
        return packageName.length() == 0 ? type : packageName + "." + type;
    }

    private boolean hasAnnotation(List<String> annotations, String name) {
        for (String annotation : annotations) {
            if (annotationName(annotation).equals(name)) {
                return true;
            }
        }
        return false;
    }

    private Route route(String annotation, String annotationName) {
        String lower = annotationName.toLowerCase(Locale.ROOT);
        if (!lower.contains("route") && !lower.contains("mapping")) {
            return null;
        }
        String method = "ANY";
        if (lower.equals("getmapping")) {
            method = "GET";
        } else if (lower.equals("postmapping")) {
            method = "POST";
        } else if (lower.equals("putmapping")) {
            method = "PUT";
        } else if (lower.equals("deletemapping")) {
            method = "DELETE";
        }
        Matcher methodMatcher = Pattern.compile("method\\s*=\\s*\"([A-Za-z]+)\"").matcher(annotation);
        if (methodMatcher.find()) {
            method = methodMatcher.group(1).toUpperCase(Locale.ROOT);
        }
        String path = "";
        Matcher pathMatcher = Pattern.compile("(?:path|value)\\s*=\\s*\"([^\"]+)\"").matcher(annotation);
        if (pathMatcher.find()) {
            path = pathMatcher.group(1);
        } else {
            Matcher directPath = Pattern.compile("@\\w+\\s*\\(\\s*\"([^\"]+)\"").matcher(annotation);
            if (directPath.find()) {
                path = directPath.group(1);
            }
        }
        return path.length() == 0 ? null : new Route(method, path);
    }

    private String annotationName(String annotation) {
        Matcher matcher = Pattern.compile("@([A-Za-z_][\\w.]*)").matcher(annotation);
        if (!matcher.find()) {
            return "";
        }
        return simpleName(matcher.group(1));
    }

    private String stripLineComment(String line) {
        int index = line.indexOf("//");
        return index >= 0 ? line.substring(0, index) : line;
    }

    private String signature(String code) {
        return code.trim().replaceAll("\\s+", " ");
    }

    private String visibility(String code) {
        String trimmed = code.trim();
        if (trimmed.startsWith("public ")) {
            return "public";
        }
        if (trimmed.startsWith("protected ")) {
            return "protected";
        }
        if (trimmed.startsWith("private ")) {
            return "private";
        }
        return "package";
    }

    private boolean isControlStatement(String code) {
        String trimmed = code.trim();
        return trimmed.startsWith("if ") || trimmed.startsWith("if(")
                || trimmed.startsWith("for ") || trimmed.startsWith("for(")
                || trimmed.startsWith("while ") || trimmed.startsWith("while(")
                || trimmed.startsWith("switch ") || trimmed.startsWith("switch(")
                || trimmed.startsWith("catch ") || trimmed.startsWith("catch(");
    }

    private boolean isControlKeyword(String name) {
        return "if".equals(name) || "for".equals(name) || "while".equals(name)
                || "switch".equals(name) || "catch".equals(name) || "return".equals(name);
    }

    private boolean isIgnoredReceiver(String receiver) {
        return "this".equals(receiver) || "super".equals(receiver);
    }

    private boolean isTypeReceiver(String receiver) {
        return receiver != null && receiver.length() > 0 && Character.isUpperCase(receiver.charAt(0));
    }

    private String simpleName(String qualified) {
        int dot = qualified.lastIndexOf('.');
        return dot >= 0 ? qualified.substring(dot + 1) : qualified;
    }

    private String packageKey(String packageName) {
        return "java_package:" + packageName;
    }

    private String javaTypeKey(String qualifiedName) {
        return "java_type:" + qualifiedName;
    }

    private String javaMethodKey(String qualifiedName) {
        return "java_method:" + qualifiedName;
    }

    private static final class MethodMatch {
        private final String name;
        private final String visibility;
        private final String parameters;

        private MethodMatch(String name, String visibility, String parameters) {
            this.name = name;
            this.visibility = visibility;
            this.parameters = parameters == null ? "" : parameters;
        }
    }

    private static final class Route {
        private final String method;
        private final String path;

        private Route(String method, String path) {
            this.method = method;
            this.path = path;
        }
    }
}
