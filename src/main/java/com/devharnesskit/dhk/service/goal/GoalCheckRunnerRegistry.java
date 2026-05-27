package com.devharnesskit.dhk.service.goal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class GoalCheckRunnerRegistry {
    private final Map<String, GoalCheckRunner> runners;

    private GoalCheckRunnerRegistry(Map<String, GoalCheckRunner> runners) {
        this.runners = Collections.unmodifiableMap(new LinkedHashMap<String, GoalCheckRunner>(runners));
    }

    public static GoalCheckRunnerRegistry defaultRegistry(GoalCheckService service) {
        Map<String, GoalCheckRunner> runners = new LinkedHashMap<String, GoalCheckRunner>();
        register(runners, new DelegatingRunner("compile") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runMavenCheck(context.connection(), context.projectRoot(), context.goal(),
                        context.checkKey(), context.policy().compileCommand(), context.now());
            }
        });
        register(runners, new DelegatingRunner("test") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runMavenCheck(context.connection(), context.projectRoot(), context.goal(),
                        context.checkKey(), context.policy().testCommand(), context.now());
            }
        });
        register(runners, new DelegatingRunner("manual-compile") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runManualVerificationCheck(context.connection(), context.projectRoot(),
                        context.goal(), context.checkKey(), "compile_scope", context.now());
            }
        });
        register(runners, new DelegatingRunner("manual-test") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runManualVerificationCheck(context.connection(), context.projectRoot(),
                        context.goal(), context.checkKey(), "test_scope", context.now());
            }
        });
        register(runners, new DelegatingRunner("verification-risk") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runVerificationRiskCheck(context.connection(), context.projectRoot(),
                        context.goal(), context.now());
            }
        });
        register(runners, new DelegatingRunner("sensitive") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runSensitiveCheck(context.connection(), context.projectRoot(),
                        context.goal(), context.now());
            }
        });
        register(runners, new DelegatingRunner("spec") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runSpecCheck(context.connection(), context.projectRoot(), context.goal(),
                        context.profile(), context.now());
            }
        });
        register(runners, new DelegatingRunner("workflow") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runWorkflowCheck(context.connection(), context.projectRoot(), context.goal(),
                        context.now(), context.policy(), context.profile());
            }
        });
        register(runners, new DelegatingRunner("graph") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runGraphCheck(context.connection(), context.projectRoot(), context.goal(),
                        context.profile(), context.now());
            }
        });
        register(runners, new DelegatingRunner("impact") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runImpactCheck(context.connection(), context.projectRoot(), context.goal(),
                        context.profile(), context.now());
            }
        });
        register(runners, new DelegatingRunner("legacy") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runLegacyCheck(context.connection(), context.projectRoot(), context.goal(),
                        context.profile(), context.now());
            }
        });
        register(runners, new DelegatingRunner("architecture") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runArchitectureCheck(context.connection(), context.projectRoot(), context.goal(),
                        context.now());
            }
        });
        register(runners, new DelegatingRunner("bdd") {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runBddCheck(context.connection(), context.projectRoot(), context.goal(),
                        context.profile(), context.policy(), context.now());
            }
        });
        register(runners, discipline(service, "think-before-coding"));
        register(runners, discipline(service, "goal-driven"));
        register(runners, discipline(service, "simplicity"));
        register(runners, discipline(service, "surgical-change"));
        return new GoalCheckRunnerRegistry(runners);
    }

    public GoalCheckRunner find(String key) {
        return runners.get(key);
    }

    public Set<String> keys() {
        return runners.keySet();
    }

    private static GoalCheckRunner discipline(final GoalCheckService service, final String key) {
        return new DelegatingRunner(key) {
            public com.devharnesskit.dhk.model.goal.GoalCheck run(GoalCheckContext context) throws Exception {
                return service.runDisciplineGateCheck(context.connection(), context.projectRoot(),
                        context.goal(), context.profile(), key, context.now());
            }
        };
    }

    private static void register(Map<String, GoalCheckRunner> runners, GoalCheckRunner runner) {
        runners.put(runner.key(), runner);
    }

    private abstract static class DelegatingRunner implements GoalCheckRunner {
        private final String key;

        private DelegatingRunner(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
