package com.devharnesskit.dhk.service.bdd.adapter;

import com.devharnesskit.dhk.model.bdd.BddEvidence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BddEvidenceAdapterRegistry {
    private final List<BddEvidenceAdapter> adapters;

    public BddEvidenceAdapterRegistry() {
        List<BddEvidenceAdapter> defaults = new ArrayList<BddEvidenceAdapter>();
        defaults.add(new ManualBddEvidenceAdapter());
        defaults.add(new JunitBddEvidenceAdapter());
        defaults.add(new ReportBddEvidenceAdapter());
        this.adapters = Collections.unmodifiableList(defaults);
    }

    public BddEvidenceAdapterRegistry(List<BddEvidenceAdapter> adapters) {
        this.adapters = Collections.unmodifiableList(new ArrayList<BddEvidenceAdapter>(adapters));
    }

    public BddEvidenceAdapter findAdapter(BddEvidence evidence) {
        for (BddEvidenceAdapter adapter : adapters) {
            if (adapter.supports(evidence)) {
                return adapter;
            }
        }
        return null;
    }

    public List<BddEvidenceAdapter> adapters() {
        return adapters;
    }
}
