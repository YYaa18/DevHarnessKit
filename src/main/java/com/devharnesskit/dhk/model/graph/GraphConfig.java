package com.devharnesskit.dhk.model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GraphConfig {
    private final String provider;
    private final String cgcCommand;
    private final List<String> include;
    private final List<String> exclude;
    private final int maxFileBytes;
    private final int maxIndexedFiles;
    private final int maxImpactDepth;
    private final int maxExportNodes;
    private final boolean loadedFromFile;

    public GraphConfig(String provider, List<String> include, List<String> exclude,
                       int maxFileBytes, int maxIndexedFiles, int maxImpactDepth,
                       int maxExportNodes, boolean loadedFromFile) {
        this(provider, "cgc", include, exclude, maxFileBytes, maxIndexedFiles, maxImpactDepth,
                maxExportNodes, loadedFromFile);
    }

    public GraphConfig(String provider, String cgcCommand, List<String> include, List<String> exclude,
                       int maxFileBytes, int maxIndexedFiles, int maxImpactDepth,
                       int maxExportNodes, boolean loadedFromFile) {
        this.provider = provider == null || provider.length() == 0 ? "lite" : provider;
        this.cgcCommand = cgcCommand == null || cgcCommand.trim().length() == 0 ? "cgc" : cgcCommand.trim();
        this.include = Collections.unmodifiableList(new ArrayList<String>(include));
        this.exclude = Collections.unmodifiableList(new ArrayList<String>(exclude));
        this.maxFileBytes = maxFileBytes;
        this.maxIndexedFiles = maxIndexedFiles;
        this.maxImpactDepth = maxImpactDepth;
        this.maxExportNodes = maxExportNodes;
        this.loadedFromFile = loadedFromFile;
    }

    public static GraphConfig defaults() {
        List<String> include = new ArrayList<String>();
        include.add("src/main/java/**");
        include.add("src/test/java/**");
        include.add("src/main/resources/**");
        include.add("src/main/webapp/**");
        include.add("pom.xml");

        List<String> exclude = new ArrayList<String>();
        exclude.add("target/**");
        exclude.add("build/**");
        exclude.add("node_modules/**");
        exclude.add(".git/**");
        exclude.add(".agents/memory/**");
        exclude.add(".agents/graph/exports/**");
        exclude.add(".agents/graph/snapshots/**");
        exclude.add(".agents/graph/cache/**");
        exclude.add("**/*.jar");
        exclude.add("**/*.class");
        exclude.add("**/*secret*");
        exclude.add("**/*password*");
        exclude.add("**/*.pem");
        exclude.add("**/*.key");
        return new GraphConfig("lite", include, exclude, 1048576, 5000, 3, 500, false);
    }

    public GraphConfig withLoadedFromFile(boolean loaded) {
        return new GraphConfig(provider, cgcCommand, include, exclude, maxFileBytes, maxIndexedFiles,
                maxImpactDepth, maxExportNodes, loaded);
    }

    public String provider() {
        return provider;
    }

    public String cgcCommand() {
        return cgcCommand;
    }

    public List<String> include() {
        return include;
    }

    public List<String> exclude() {
        return exclude;
    }

    public int maxFileBytes() {
        return maxFileBytes;
    }

    public int maxIndexedFiles() {
        return maxIndexedFiles;
    }

    public int maxImpactDepth() {
        return maxImpactDepth;
    }

    public int maxExportNodes() {
        return maxExportNodes;
    }

    public boolean loadedFromFile() {
        return loadedFromFile;
    }
}
