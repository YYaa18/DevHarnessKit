package com.devharnesskit.dhk.model.graph;

import com.devharnesskit.dhk.model.bdd.BddBinding;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScenarioGraphImpactResult {
    private final String scenarioKey;
    private final List<BddBinding> inputBindings;
    private final List<GraphImpactResult> impactResults;
    private final Path scenarioImpactMapPath;

    public ScenarioGraphImpactResult(String scenarioKey, List<BddBinding> inputBindings,
                                     List<GraphImpactResult> impactResults, Path scenarioImpactMapPath) {
        this.scenarioKey = scenarioKey == null ? "" : scenarioKey;
        this.inputBindings = Collections.unmodifiableList(new ArrayList<BddBinding>(inputBindings));
        this.impactResults = Collections.unmodifiableList(new ArrayList<GraphImpactResult>(impactResults));
        this.scenarioImpactMapPath = scenarioImpactMapPath;
    }

    public String scenarioKey() {
        return scenarioKey;
    }

    public List<BddBinding> inputBindings() {
        return inputBindings;
    }

    public List<GraphImpactResult> impactResults() {
        return impactResults;
    }

    public Path scenarioImpactMapPath() {
        return scenarioImpactMapPath;
    }

    public boolean found() {
        return foundCount() > 0;
    }

    public int foundCount() {
        int count = 0;
        for (GraphImpactResult result : impactResults) {
            if (result.found()) {
                count++;
            }
        }
        return count;
    }

    public List<String> relatedFiles() {
        Set<String> values = new LinkedHashSet<String>();
        for (GraphImpactResult result : impactResults) {
            values.addAll(result.relatedFiles());
        }
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }

    public List<String> relatedTests() {
        Set<String> values = new LinkedHashSet<String>();
        for (GraphImpactResult result : impactResults) {
            values.addAll(result.relatedTests());
        }
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }

    public List<GraphNode> relatedSql() {
        Map<String, GraphNode> values = new LinkedHashMap<String, GraphNode>();
        for (GraphImpactResult result : impactResults) {
            for (GraphNode node : result.relatedSql()) {
                values.put(node.nodeKey(), node);
            }
        }
        return Collections.unmodifiableList(new ArrayList<GraphNode>(values.values()));
    }

    public List<GraphNode> riskNodes() {
        Map<String, GraphNode> values = new LinkedHashMap<String, GraphNode>();
        for (GraphImpactResult result : impactResults) {
            for (GraphNode node : result.riskNodes()) {
                values.put(node.nodeKey(), node);
            }
        }
        return Collections.unmodifiableList(new ArrayList<GraphNode>(values.values()));
    }

    public List<String> recommendedReadFiles() {
        Set<String> values = new LinkedHashSet<String>();
        for (GraphImpactResult result : impactResults) {
            values.addAll(result.recommendedReadFiles());
        }
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }

    public boolean snapshotStale() {
        for (GraphImpactResult result : impactResults) {
            if (result.snapshotStale()) {
                return true;
            }
        }
        return false;
    }

    public boolean staleAllowed() {
        for (GraphImpactResult result : impactResults) {
            if (result.staleAllowed()) {
                return true;
            }
        }
        return false;
    }
}
