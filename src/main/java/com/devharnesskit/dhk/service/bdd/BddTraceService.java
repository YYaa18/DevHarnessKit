package com.devharnesskit.dhk.service.bdd;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.bdd.BddEvidence;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.repository.bdd.BddBindingRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class BddTraceService {
    private final BddBindingRepository bindingRepository;
    private final BddService bddService;

    public BddTraceService() {
        this(new BddBindingRepository(), new BddService(new BddFeatureRepository(),
                new BddScenarioRepository(), new BddStepRepository()));
    }

    BddTraceService(BddBindingRepository bindingRepository, BddService bddService) {
        this.bindingRepository = bindingRepository;
        this.bddService = bddService;
    }

    public List<String> specTrace(Connection connection, Project project, String changeKey,
                                  List<SpecAcceptance> acceptances) throws SQLException {
        List<String> result = new ArrayList<String>();
        for (SpecAcceptance acceptance : acceptances) {
            List<BddBinding> bindings = bindingRepository.listByBinding(connection,
                    "spec_acceptance", changeKey + ":" + acceptance.acceptanceKey());
            for (BddBinding binding : bindings) {
                result.add(acceptance.acceptanceKey() + " -> "
                        + traceLine(connection, project, binding));
            }
        }
        return result;
    }

    public List<String> workflowTrace(Connection connection, Project project, String runKey)
            throws SQLException {
        List<String> result = new ArrayList<String>();
        List<BddBinding> bindings = bindingRepository.listByBinding(connection, "workflow", runKey);
        for (BddBinding binding : bindings) {
            result.add(traceLine(connection, project, binding));
        }
        return result;
    }

    private String traceLine(Connection connection, Project project, BddBinding binding)
            throws SQLException {
        BddScenarioView view = bddService.findScenario(connection, project, binding.scenarioKey());
        if (view == null) {
            return binding.scenarioKey() + " [missing] relation=" + binding.relation();
        }
        List<BddEvidence> evidence = bddService.listEvidence(connection, view.scenario().scenarioKey(), "");
        BddEvidence latest = evidence.isEmpty() ? null : evidence.get(0);
        String evidenceText = latest == null
                ? "missing"
                : latest.status() + " " + latest.evidenceType() + ": " + latest.summary();
        return view.scenario().scenarioKey() + " [" + view.scenario().status() + "] "
                + view.scenario().title() + " relation=" + binding.relation()
                + " latest_evidence=" + evidenceText;
    }
}
