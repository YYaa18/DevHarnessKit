package com.devharnesskit.dhk.model.bdd;

import java.util.Collections;
import java.util.List;

public final class BddScenarioView {
    private final BddFeature feature;
    private final BddScenario scenario;
    private final List<BddStep> steps;
    private final List<BddBinding> bindings;
    private final List<BddEvidence> evidence;

    public BddScenarioView(BddFeature feature, BddScenario scenario, List<BddStep> steps) {
        this(feature, scenario, steps, Collections.<BddBinding>emptyList(),
                Collections.<BddEvidence>emptyList());
    }

    public BddScenarioView(BddFeature feature, BddScenario scenario, List<BddStep> steps,
                           List<BddBinding> bindings) {
        this(feature, scenario, steps, bindings, Collections.<BddEvidence>emptyList());
    }

    public BddScenarioView(BddFeature feature, BddScenario scenario, List<BddStep> steps,
                           List<BddBinding> bindings, List<BddEvidence> evidence) {
        this.feature = feature;
        this.scenario = scenario;
        this.steps = Collections.unmodifiableList(steps);
        this.bindings = Collections.unmodifiableList(bindings);
        this.evidence = Collections.unmodifiableList(evidence);
    }

    public BddFeature feature() {
        return feature;
    }

    public BddScenario scenario() {
        return scenario;
    }

    public List<BddStep> steps() {
        return steps;
    }

    public List<BddBinding> bindings() {
        return bindings;
    }

    public List<BddEvidence> evidence() {
        return evidence;
    }
}
