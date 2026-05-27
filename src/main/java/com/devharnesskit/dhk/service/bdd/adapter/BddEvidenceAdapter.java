package com.devharnesskit.dhk.service.bdd.adapter;

import com.devharnesskit.dhk.model.bdd.BddAdapterResult;
import com.devharnesskit.dhk.model.bdd.BddEvidence;

public interface BddEvidenceAdapter {
    String key();

    boolean supports(BddEvidence evidence);

    BddAdapterResult normalize(BddEvidence evidence);
}
