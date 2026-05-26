package com.devharnesskit.dhk.model.graph;

public final class GraphArchitectureConfig {
    private final String mode;
    private final String[] controllerPatterns;
    private final String[] servicePatterns;
    private final String[] repositoryPatterns;
    private final String[] domainPatterns;
    private final String[] dtoPatterns;
    private final String[] publicApiPatterns;
    private final String[] forbiddenDependencies;

    public GraphArchitectureConfig(String mode,
                                   String[] controllerPatterns,
                                   String[] servicePatterns,
                                   String[] repositoryPatterns,
                                   String[] domainPatterns,
                                   String[] dtoPatterns,
                                   String[] publicApiPatterns,
                                   String[] forbiddenDependencies) {
        this.mode = value(mode).length() == 0 ? "warn" : value(mode);
        this.controllerPatterns = array(controllerPatterns);
        this.servicePatterns = array(servicePatterns);
        this.repositoryPatterns = array(repositoryPatterns);
        this.domainPatterns = array(domainPatterns);
        this.dtoPatterns = array(dtoPatterns);
        this.publicApiPatterns = array(publicApiPatterns);
        this.forbiddenDependencies = array(forbiddenDependencies);
    }

    public static GraphArchitectureConfig defaults() {
        return new GraphArchitectureConfig("warn",
                new String[]{"src/main/java/**/controller/**"},
                new String[]{"src/main/java/**/service/**"},
                new String[]{"src/main/java/**/repository/**"},
                new String[]{"src/main/java/**/domain/**"},
                new String[]{"src/main/java/**/dto/**"},
                new String[]{"src/main/java/**/controller/**"},
                new String[]{"controller->repository", "repository->controller",
                        "repository->service", "service->controller"});
    }

    public String mode() { return mode; }
    public boolean failMode() { return "fail".equals(mode); }
    public String[] controllerPatterns() { return controllerPatterns; }
    public String[] servicePatterns() { return servicePatterns; }
    public String[] repositoryPatterns() { return repositoryPatterns; }
    public String[] domainPatterns() { return domainPatterns; }
    public String[] dtoPatterns() { return dtoPatterns; }
    public String[] publicApiPatterns() { return publicApiPatterns; }
    public String[] forbiddenDependencies() { return forbiddenDependencies; }

    private static String value(String input) {
        return input == null ? "" : input.trim();
    }

    private static String[] array(String[] values) {
        return values == null ? new String[0] : values;
    }
}
