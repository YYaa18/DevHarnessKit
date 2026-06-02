package com.devharnesskit.dhk.db.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DefaultMigrationStepCatalog {
    private static final List<MigrationStep> STEPS = build();

    private DefaultMigrationStepCatalog() {
    }

    public static List<MigrationStep> steps() {
        return STEPS;
    }

    private static List<MigrationStep> build() {
        List<MigrationStep> result = new ArrayList<MigrationStep>();
        result.add(new V1InitialMemoryMigration());
        result.add(new V2WorkflowPersistenceMigration());
        result.add(new V3WorkflowArtifactBindingMigration());
        result.add(new V4SpecPersistenceMigration());
        result.add(new V5GoalOrchestrationMigration());
        result.add(new V6GoalCheckFreshnessMigration());
        result.add(new V7GoalContextExportFailureMigration());
        result.add(new V8GoalWorkflowSpecSyncMigration());
        result.add(new V9GraphLiteMigration());
        result.add(new V10BddAcceptanceMigration());
        result.add(new V11SkillContractMigration());
        result.add(new V12HumanCheckpointMigration());
        result.add(new V13SkillTrustHardeningMigration());
        result.add(new V14BriefLifecycleSQLiteMigration());
        result.add(new V15KnowledgeCandidateMetadataMigration());
        result.add(new V16GoalExternalRefMigration());
        result.add(new V17MemoryQualityMigration());
        return Collections.unmodifiableList(result);
    }
}
