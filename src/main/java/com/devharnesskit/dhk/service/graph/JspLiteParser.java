package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphParseResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JspLiteParser implements GraphSourceParser {
    private static final Pattern INCLUDE = Pattern.compile("<%@\\s*include\\s+file\\s*=\\s*\"([^\"]+)\"\\s*%>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FORM = Pattern.compile("<form\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern INPUT = Pattern.compile("<(?:input|select|textarea)\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HREF = Pattern.compile("\\bhref\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR = Pattern.compile("([A-Za-z_][\\w.-]*)\\s*=\\s*(['\"])(.*?)\\2");

    public boolean supports(GraphFileEntry entry) {
        return "jsp".equals(entry.language());
    }

    public void parse(Path projectRoot, GraphFileEntry entry, GraphParseResult.Builder builder) throws Exception {
        Path file = projectRoot.resolve(entry.relativePath());
        String content = new String(Files.readAllBytes(file), "UTF-8");
        String pageKey = jspKey(entry.relativePath());
        builder.addNode(new GraphNode(pageKey, "jsp_page", entry.relativePath(), entry.relativePath(),
                entry.relativePath(), 1, 1, "jsp", "", "", 85, "lite", "JSP page"));
        addIncludes(builder, entry, content, pageKey);
        addForms(builder, entry, content, pageKey);
        addLinks(builder, entry, content, pageKey);
    }

    private void addIncludes(GraphParseResult.Builder builder, GraphFileEntry entry, String content,
                             String pageKey) {
        Matcher matcher = INCLUDE.matcher(content);
        while (matcher.find()) {
            String includePath = normalizeJspPath(matcher.group(1), entry.relativePath());
            String includeKey = jspKey(includePath);
            int line = lineOf(content, matcher.start());
            builder.addNode(new GraphNode(includeKey, "jsp_page", includePath, includePath,
                    includePath, line, line, "jsp", "", "", 70, "lite", "JSP include target"));
            builder.addEdge(new GraphEdge("includes", pageKey, includeKey, entry.relativePath(), 80,
                    "lite", "JSP include directive"));
        }
    }

    private void addForms(GraphParseResult.Builder builder, GraphFileEntry entry, String content, String pageKey) {
        Matcher matcher = FORM.matcher(content);
        while (matcher.find()) {
            String attrs = matcher.group(1);
            String id = attr(attrs, "id");
            String action = normalizeAction(attr(attrs, "action"));
            String method = attr(attrs, "method").toUpperCase(java.util.Locale.ROOT);
            if (method.length() == 0) {
                method = "GET";
            }
            int line = lineOf(content, matcher.start());
            String formName = id.length() == 0 ? action : id;
            String formKey = "jsp_form:" + entry.relativePath() + "#" + formName;
            builder.addNode(new GraphNode(formKey, "jsp_form", formName, formName,
                    entry.relativePath(), line, line, "jsp", "", method + " " + action, 85, "lite",
                    "JSP form declaration"));
            builder.addEdge(new GraphEdge("contains", pageKey, formKey, entry.relativePath(), 85,
                    "lite", "JSP contains form"));
            if (action.length() > 0) {
                String routeKey = routeKey("ANY", action);
                builder.addNode(new GraphNode(routeKey, "route", action, action,
                        entry.relativePath(), line, line, "jsp", "", "", 80, "lite", "JSP form action"));
                builder.addEdge(new GraphEdge("submits_to", formKey, routeKey, entry.relativePath(), 85,
                        "lite", "JSP form action"));
            }
            addFormFields(builder, entry, content, formKey, matcher.end());
        }
    }

    private void addFormFields(GraphParseResult.Builder builder, GraphFileEntry entry, String content,
                               String formKey, int formStart) {
        int formEnd = lower(content).indexOf("</form>", formStart);
        String formBody = formEnd < 0 ? content.substring(formStart) : content.substring(formStart, formEnd);
        Matcher matcher = INPUT.matcher(formBody);
        while (matcher.find()) {
            String name = attr(matcher.group(1), "name");
            if (name.length() == 0) {
                continue;
            }
            int line = lineOf(content, formStart + matcher.start());
            String fieldKey = "form_field:" + entry.relativePath() + "#" + name;
            builder.addNode(new GraphNode(fieldKey, "form_field", name, name, entry.relativePath(),
                    line, line, "jsp", "", "", 80, "lite", "JSP form field"));
            builder.addEdge(new GraphEdge("contains", formKey, fieldKey, entry.relativePath(), 85,
                    "lite", "JSP form contains field"));
        }
    }

    private void addLinks(GraphParseResult.Builder builder, GraphFileEntry entry, String content, String pageKey) {
        Matcher matcher = HREF.matcher(content);
        while (matcher.find()) {
            String href = normalizeAction(matcher.group(1));
            if (href.length() == 0 || href.startsWith("#")) {
                continue;
            }
            int line = lineOf(content, matcher.start());
            String routeKey = routeKey("ANY", href);
            builder.addNode(new GraphNode(routeKey, "route", href, href,
                    entry.relativePath(), line, line, "jsp", "", "", 70, "lite", "JSP link href"));
            builder.addEdge(new GraphEdge("links_to", pageKey, routeKey, entry.relativePath(), 70,
                    "lite", "JSP href"));
        }
    }

    private String attr(String attrs, String name) {
        Matcher matcher = ATTR.matcher(attrs == null ? "" : attrs);
        while (matcher.find()) {
            if (name.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(3).trim();
            }
        }
        return "";
    }

    private String normalizeAction(String raw) {
        String value = raw == null ? "" : raw.trim();
        value = value.replace("${pageContext.request.contextPath}", "");
        value = value.replace("${request.contextPath}", "");
        return value;
    }

    private String normalizeJspPath(String raw, String currentPath) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("/")) {
            return "src/main/webapp" + value;
        }
        int slash = currentPath.lastIndexOf('/');
        return slash >= 0 ? currentPath.substring(0, slash + 1) + value : value;
    }

    private String jspKey(String path) {
        return "jsp_page:" + path;
    }

    private String routeKey(String method, String path) {
        return "route:" + method + ":" + path;
    }

    private int lineOf(String content, int offset) {
        int line = 1;
        int max = Math.min(content.length(), Math.max(0, offset));
        for (int index = 0; index < max; index++) {
            if (content.charAt(index) == '\n') {
                line++;
            }
        }
        return line;
    }

    private String lower(String value) {
        return value.toLowerCase(java.util.Locale.ROOT);
    }
}
