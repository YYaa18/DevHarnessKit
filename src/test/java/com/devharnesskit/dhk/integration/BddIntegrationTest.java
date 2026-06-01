package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BddIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void bddInitAddListShowAndJsonOutput() throws Exception {
        Harness init = new Harness(tempDir);
        int initExit = new CommandRouter().run(new String[]{
                "bdd", "init", "--project-root", "demo"
        }, init.context());
        Path root = tempDir.resolve("demo");
        assertEquals(ExitCodes.SUCCESS, initExit);
        assertTrue(init.stdout().contains("bdd init complete"));
        assertTrue(Files.isDirectory(PathUtil.bddFeaturesDirectory(root)));
        assertTrue(Files.isDirectory(PathUtil.bddEvidenceDirectory(root)));
        assertTrue(Files.isDirectory(PathUtil.bddExportsDirectory(root)));
        assertEquals(1, countRows("schema_version", "version = " + MigrationRunner.V10));

        Harness add = new Harness(tempDir);
        int addExit = new CommandRouter().run(new String[]{
                "bdd", "add", "--project-root", "demo",
                "--feature", "order-query",
                "--title", "订单查询",
                "--scenario", "order-query-happy-path",
                "--scenario-title", "分页查询订单成功",
                "--module", "order",
                "--tags", "api,order",
                "--given", "已有订单数据",
                "--when", "用户按分页条件查询",
                "--then", "返回订单分页结果"
        }, add.context());
        assertEquals(ExitCodes.SUCCESS, addExit);
        assertTrue(add.stdout().contains("feature_key: order-query"));
        assertTrue(add.stdout().contains("scenario_key: order-query-happy-path"));
        assertTrue(add.stdout().contains("status: active"));
        assertTrue(add.stdout().contains("steps: 3"));
        assertEquals(1, countRows("bdd_feature", "feature_key = 'order-query'"));
        assertEquals(1, countRows("bdd_scenario",
                "scenario_key = 'order-query-happy-path' AND status = 'active'"));
        assertEquals(3, countRows("bdd_step", "scenario_key = 'order-query-happy-path'"));

        Harness list = new Harness(tempDir);
        int listExit = new CommandRouter().run(new String[]{
                "bdd", "list", "--project-root", "demo"
        }, list.context());
        assertEquals(ExitCodes.SUCCESS, listExit);
        assertTrue(list.stdout().contains("- order-query [active] 订单查询 (order)"));
        assertTrue(list.stdout().contains("order-query-happy-path [active] 分页查询订单成功"));

        Harness show = new Harness(tempDir);
        int showExit = new CommandRouter().run(new String[]{
                "bdd", "show", "--project-root", "demo", "--scenario", "order-query-happy-path"
        }, show.context());
        assertEquals(ExitCodes.SUCCESS, showExit);
        assertTrue(show.stdout().contains("scenario_title: 分页查询订单成功"));
        assertTrue(show.stdout().contains("- given: 已有订单数据"));
        assertTrue(show.stdout().contains("- when: 用户按分页条件查询"));
        assertTrue(show.stdout().contains("- then: 返回订单分页结果"));

        Harness json = new Harness(tempDir);
        int jsonExit = new CommandRouter().run(new String[]{
                "bdd", "show", "--project-root", "demo", "--scenario", "order-query-happy-path", "--json"
        }, json.context());
        assertEquals(ExitCodes.SUCCESS, jsonExit);
        assertTrue(json.stdout().contains("\"command\": \"bdd show\""));
        assertTrue(json.stdout().contains("\"scenario_key\": \"order-query-happy-path\""));
        assertTrue(json.stdout().contains("\"steps\": ["));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "bdd", "export", "--project-root", "demo"
        }, export.context());
        assertEquals(ExitCodes.SUCCESS, exportExit);
        assertTrue(export.stdout().contains("bdd export complete"));
        assertTrue(export.stdout().contains("feature_files: 1"));
        String context = new String(Files.readAllBytes(PathUtil.bddContext(root)), "UTF-8");
        assertTrue(context.contains("# BDD_CONTEXT"));
        assertTrue(context.contains("<boundary>"));
        assertTrue(context.contains("order-query-happy-path [active] 分页查询订单成功"));
        assertTrue(context.contains("Do not claim behavior is verified until evidence and checks pass"));
        String featureFile = new String(Files.readAllBytes(
                PathUtil.bddFeaturesDirectory(root).resolve("order-query.feature")), "UTF-8");
        assertTrue(featureFile.contains("Feature: 订单查询"));
        assertTrue(featureFile.contains("Scenario: 分页查询订单成功"));
        assertTrue(featureFile.contains("Given 已有订单数据"));
        assertTrue(featureFile.contains("When 用户按分页条件查询"));
        assertTrue(featureFile.contains("Then 返回订单分页结果"));

        Harness jsonExport = new Harness(tempDir);
        int jsonExportExit = new CommandRouter().run(new String[]{
                "bdd", "export", "--project-root", "demo", "--feature", "order-query", "--json"
        }, jsonExport.context());
        assertEquals(ExitCodes.SUCCESS, jsonExportExit);
        assertTrue(jsonExport.stdout().contains("\"command\": \"bdd export\""));
        assertTrue(jsonExport.stdout().contains("\"feature_files\": 1"));
    }

    @Test
    void bddScenarioAliasCreatesListsShowsAndRejectsDuplicates() throws Exception {
        Harness create = new Harness(tempDir);
        int createExit = new CommandRouter().run(new String[]{
                "bdd", "scenario", "create", "--project-root", "demo",
                "--module", "address",
                "--scenario", "address-validation-basic",
                "--title", "新增配送地址需要校验必填字段",
                "--given", "用户进入新增配送地址页面",
                "--when", "用户未填写收件人或手机号直接提交",
                "--then", "系统提示必填字段错误"
        }, create.context());
        assertEquals(ExitCodes.SUCCESS, createExit);
        assertTrue(create.stdout().contains("scenario_key: address-validation-basic"));
        assertTrue(create.stdout().contains("status: active"));

        Harness list = new Harness(tempDir);
        int listExit = new CommandRouter().run(new String[]{
                "bdd", "scenario", "list", "--project-root", "demo"
        }, list.context());
        assertEquals(ExitCodes.SUCCESS, listExit);
        assertTrue(list.stdout().contains("address-validation-basic [active] 新增配送地址需要校验必填字段"));

        Harness show = new Harness(tempDir);
        int showExit = new CommandRouter().run(new String[]{
                "bdd", "scenario", "show", "--project-root", "demo",
                "--scenario", "address-validation-basic"
        }, show.context());
        assertEquals(ExitCodes.SUCCESS, showExit);
        assertTrue(show.stdout().contains("- given: 用户进入新增配送地址页面"));
        assertTrue(show.stdout().contains("- when: 用户未填写收件人或手机号直接提交"));
        assertTrue(show.stdout().contains("- then: 系统提示必填字段错误"));

        Harness duplicate = new Harness(tempDir);
        int duplicateExit = new CommandRouter().run(new String[]{
                "bdd", "scenario", "create", "--project-root", "demo",
                "--scenario", "address-validation-basic",
                "--title", "重复场景"
        }, duplicate.context());
        assertEquals(ExitCodes.USAGE_ERROR, duplicateExit);
        assertTrue(duplicate.stderr().contains("error_code: BDD_SCENARIO_ALREADY_EXISTS"));
        assertEquals(1, countRows("bdd_scenario", "scenario_key = 'address-validation-basic'"));
    }

    @Test
    void bddEnumErrorsIncludeValidValues() {
        Harness invalidScenarioType = new Harness(tempDir);
        int invalidScenarioTypeExit = new CommandRouter().run(new String[]{
                "bdd", "add", "--project-root", "demo",
                "--feature", "order-query",
                "--title", "订单查询",
                "--scenario", "order-query-happy-path",
                "--scenario-title", "分页查询订单成功",
                "--type", "robot"
        }, invalidScenarioType.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, invalidScenarioTypeExit);
        assertTrue(invalidScenarioType.stderr().contains("error_code: INVALID_BDD_SCENARIO_TYPE"));
        assertTrue(invalidScenarioType.stderr().contains("valid_values:"));
        assertTrue(invalidScenarioType.stderr().contains("acceptance"));

        Harness add = new Harness(tempDir);
        int addExit = new CommandRouter().run(new String[]{
                "bdd", "add", "--project-root", "demo",
                "--feature", "order-query",
                "--title", "订单查询",
                "--scenario", "order-query-happy-path",
                "--scenario-title", "分页查询订单成功"
        }, add.context());
        assertEquals(ExitCodes.SUCCESS, addExit);

        Harness invalidEvidenceStatus = new Harness(tempDir);
        int invalidEvidenceStatusExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--status", "complete",
                "--summary", "Manual check passed"
        }, invalidEvidenceStatus.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, invalidEvidenceStatusExit);
        assertTrue(invalidEvidenceStatus.stderr().contains("error_code: INVALID_BDD_EVIDENCE_STATUS"));
        assertTrue(invalidEvidenceStatus.stderr().contains("valid_values:"));
        assertTrue(invalidEvidenceStatus.stderr().contains("passed"));
    }

    @Test
    void bddUsageInvalidKeyAndNotFoundErrorsAreActionable() {
        Harness missingAddArgs = new Harness(tempDir);
        int missingAddArgsExit = new CommandRouter().run(new String[]{
                "bdd", "add", "--project-root", "demo", "--feature", "order-query"
        }, missingAddArgs.context());
        assertEquals(ExitCodes.USAGE_ERROR, missingAddArgsExit);
        assertTrue(missingAddArgs.stderr().contains("error_code: BDD_ADD_ARGUMENTS_MISSING"));
        assertTrue(missingAddArgs.stderr().contains("missing:"));
        assertTrue(missingAddArgs.stderr().contains("--scenario-title"));

        Harness invalidKey = new Harness(tempDir);
        int invalidKeyExit = new CommandRouter().run(new String[]{
                "bdd", "add", "--project-root", "demo",
                "--feature", "order/query",
                "--title", "订单查询",
                "--scenario", "order-query-happy-path",
                "--scenario-title", "分页查询订单成功"
        }, invalidKey.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, invalidKeyExit);
        assertTrue(invalidKey.stderr().contains("error_code: BDD_ADD_INVALID_KEY"));
        assertTrue(invalidKey.stderr().contains("next_command:"));

        Harness showMissing = new Harness(tempDir);
        int showMissingExit = new CommandRouter().run(new String[]{
                "bdd", "show", "--project-root", "demo", "--scenario", "missing-scenario"
        }, showMissing.context());
        assertEquals(ExitCodes.NOT_FOUND, showMissingExit);
        assertTrue(showMissing.stderr().contains("error_code: BDD_SCENARIO_NOT_FOUND"));
        assertTrue(showMissing.stderr().contains("next_action:"));

        Harness evidenceMissingArgs = new Harness(tempDir);
        int evidenceMissingArgsExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo", "--scenario", "missing-scenario"
        }, evidenceMissingArgs.context());
        assertEquals(ExitCodes.USAGE_ERROR, evidenceMissingArgsExit);
        assertTrue(evidenceMissingArgs.stderr().contains("error_code: BDD_EVIDENCE_ADD_ARGUMENTS_MISSING"));
    }

    @Test
    void bddAddMissingCoreStepsCreatesDraftAndSensitiveInputIsRejected() throws Exception {
        Harness draft = new Harness(tempDir);
        int draftExit = new CommandRouter().run(new String[]{
                "bdd", "add", "--project-root", "demo",
                "--feature", "risk-review",
                "--title", "风险复核",
                "--scenario", "risk-review-draft",
                "--scenario-title", "缺少完整 Given When Then",
                "--given", "已有风险订单"
        }, draft.context());
        assertEquals(ExitCodes.SUCCESS, draftExit);
        assertTrue(draft.stdout().contains("status: draft"));
        assertTrue(draft.stdout().contains("lint_warning: missing Given/When/Then"));
        assertEquals(1, countRows("bdd_scenario",
                "scenario_key = 'risk-review-draft' AND status = 'draft'"));
        assertEquals(1, countRows("bdd_step", "scenario_key = 'risk-review-draft'"));

        Harness sensitive = new Harness(tempDir);
        int sensitiveExit = new CommandRouter().run(new String[]{
                "bdd", "add", "--project-root", "demo",
                "--feature", "secret-feature",
                "--title", "Secret feature",
                "--scenario", "secret-scenario",
                "--scenario-title", "Secret scenario",
                "--then", "password=abc"
        }, sensitive.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, sensitiveExit);
        assertTrue(sensitive.stderr().contains("Sensitive data rejected in bdd add"));
        assertEquals(0, countRows("bdd_scenario", "scenario_key = 'secret-scenario'"));
    }

    @Test
    void bddLintRecordsMissingVagueAndDuplicateScenarioIssues() throws Exception {
        Harness first = new Harness(tempDir);
        int firstExit = new CommandRouter().run(new String[]{
                "bdd", "add", "--project-root", "demo",
                "--feature", "risk-lint",
                "--title", "风险 lint",
                "--scenario", "risk-lint-one",
                "--scenario-title", "测试场景",
                "--given", "准备数据",
                "--when", "用户操作",
                "--then", "系统正常"
        }, first.context());
        assertEquals(ExitCodes.SUCCESS, firstExit);

        Harness duplicate = new Harness(tempDir);
        int duplicateExit = new CommandRouter().run(new String[]{
                "bdd", "add", "--project-root", "demo",
                "--feature", "risk-lint",
                "--title", "风险 lint",
                "--scenario", "risk-lint-two",
                "--scenario-title", "测试场景",
                "--given", "存在一笔金额超过审批阈值的订单"
        }, duplicate.context());
        assertEquals(ExitCodes.SUCCESS, duplicateExit);

        Harness lint = new Harness(tempDir);
        int lintExit = new CommandRouter().run(new String[]{
                "bdd", "lint", "--project-root", "demo", "--feature", "risk-lint"
        }, lint.context());
        assertEquals(ExitCodes.SUCCESS, lintExit);
        assertTrue(lint.stdout().contains("bdd lint complete"));
        assertTrue(lint.stdout().contains("vague_step"));
        assertTrue(lint.stdout().contains("missing_when"));
        assertTrue(lint.stdout().contains("missing_then"));
        assertTrue(lint.stdout().contains("duplicate_scenario_title"));
        assertTrue(lint.stdout().contains("vague_scenario"));
        assertTrue(lint.stdout().contains("missing_acceptance_mapping"));
        assertEquals(10, countRows("bdd_quality_issue", "feature_key = 'risk-lint' AND status = 'open'"));

        Harness json = new Harness(tempDir);
        int jsonExit = new CommandRouter().run(new String[]{
                "bdd", "lint", "--project-root", "demo", "--feature", "risk-lint", "--json"
        }, json.context());
        assertEquals(ExitCodes.SUCCESS, jsonExit);
        assertTrue(json.stdout().contains("\"command\": \"bdd lint\""));
        assertTrue(json.stdout().contains("\"issue_count\": 10"));
        assertTrue(json.stdout().contains("\"issue_type\": \"vague_step\""));
        assertTrue(json.stdout().contains("\"issue_type\": \"missing_acceptance_mapping\""));
    }

    @Test
    void bddBindSpecAndGoalExposeScenarioTraceability() throws Exception {
        addOrderScenario();

        Harness spec = new Harness(tempDir);
        int specExit = new CommandRouter().run(new String[]{
                "spec", "create", "--project-root", "demo",
                "--change", "order-query-api",
                "--title", "订单查询接口",
                "--summary", "补充订单分页查询验收",
                "--module", "order",
                "--mode", "api"
        }, spec.context());
        assertEquals(ExitCodes.SUCCESS, specExit);

        Harness acceptance = new Harness(tempDir);
        int acceptanceExit = new CommandRouter().run(new String[]{
                "spec", "acceptance", "add", "--project-root", "demo",
                "--change", "order-query-api",
                "--acceptance", "A001",
                "--description", "分页查询返回订单列表"
        }, acceptance.context());
        assertEquals(ExitCodes.SUCCESS, acceptanceExit);

        Harness goal = new Harness(tempDir);
        int goalExit = new CommandRouter().run(new String[]{
                "goal", "start", "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "订单查询 BDD 绑定",
                "--module", "order"
        }, goal.context());
        assertEquals(ExitCodes.SUCCESS, goalExit);
        String goalKey = valueAfter(goal.stdout(), "goal_key: ");
        assertTrue(goalKey.length() > 0);

        Harness bindSpec = new Harness(tempDir);
        int bindSpecExit = new CommandRouter().run(new String[]{
                "bdd", "bind-spec", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--change", "order-query-api",
                "--acceptance", "A001"
        }, bindSpec.context());
        assertEquals(ExitCodes.SUCCESS, bindSpecExit);
        assertTrue(bindSpec.stdout().contains("bdd bind-spec complete"));
        assertTrue(bindSpec.stdout().contains("binding_key: order-query-api:A001"));

        Harness bindSpecAgain = new Harness(tempDir);
        int bindSpecAgainExit = new CommandRouter().run(new String[]{
                "bdd", "bind-spec", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--change", "order-query-api",
                "--acceptance", "A001"
        }, bindSpecAgain.context());
        assertEquals(ExitCodes.SUCCESS, bindSpecAgainExit);
        assertEquals(1, countRows("bdd_binding",
                "scenario_key = 'order-query-happy-path' AND binding_type = 'spec_acceptance' "
                        + "AND binding_key = 'order-query-api:A001'"));

        Harness bindGoal = new Harness(tempDir);
        int bindGoalExit = new CommandRouter().run(new String[]{
                "bdd", "bind-goal", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--goal", goalKey,
                "--json"
        }, bindGoal.context());
        assertEquals(ExitCodes.SUCCESS, bindGoalExit);
        assertTrue(bindGoal.stdout().contains("\"command\": \"bdd bind-goal\""));
        assertTrue(bindGoal.stdout().contains("\"binding_type\": \"goal\""));
        assertEquals(2, countRows("bdd_binding", "scenario_key = 'order-query-happy-path'"));

        Harness show = new Harness(tempDir);
        int showExit = new CommandRouter().run(new String[]{
                "bdd", "show", "--project-root", "demo", "--scenario", "order-query-happy-path"
        }, show.context());
        assertEquals(ExitCodes.SUCCESS, showExit);
        assertTrue(show.stdout().contains("bindings:"));
        assertTrue(show.stdout().contains("- spec_acceptance order-query-api:A001 (verifies)"));
        assertTrue(show.stdout().contains("- goal " + goalKey + " (supports)"));

        Harness jsonShow = new Harness(tempDir);
        int jsonShowExit = new CommandRouter().run(new String[]{
                "bdd", "show", "--project-root", "demo", "--scenario", "order-query-happy-path", "--json"
        }, jsonShow.context());
        assertEquals(ExitCodes.SUCCESS, jsonShowExit);
        assertTrue(jsonShow.stdout().contains("\"bindings\": ["));
        assertTrue(jsonShow.stdout().contains("\"binding_key\": \"order-query-api:A001\""));
        assertTrue(jsonShow.stdout().contains("\"binding_key\": \"" + goalKey + "\""));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "bdd", "export", "--project-root", "demo"
        }, export.context());
        assertEquals(ExitCodes.SUCCESS, exportExit);
        String context = new String(Files.readAllBytes(PathUtil.bddContext(tempDir.resolve("demo"))), "UTF-8");
        assertTrue(context.contains("bindings:"));
        assertTrue(context.contains("- spec_acceptance order-query-api:A001 (verifies)"));
        assertTrue(context.contains("- goal " + goalKey + " (supports)"));
        String featureFile = new String(Files.readAllBytes(
                PathUtil.bddFeaturesDirectory(tempDir.resolve("demo")).resolve("order-query.feature")), "UTF-8");
        assertTrue(featureFile.contains("# bindings: goal " + goalKey + " (supports); "
                + "spec_acceptance order-query-api:A001 (verifies)")
                || featureFile.contains("# bindings: spec_acceptance order-query-api:A001 (verifies); "
                + "goal " + goalKey + " (supports)"));
    }

    @Test
    void bddVerifyGoalAggregatesOnlyGoalBoundScenarios() throws Exception {
        addScenario("order-bound", "已绑定场景",
                "已有订单", "用户查询订单", "返回订单结果");
        addScenario("order-unbound", "未绑定场景",
                "已有订单", "用户删除订单", "订单被删除");
        Path root = tempDir.resolve("demo");
        writeBddRequiredProfile(root);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start", "--project-root", "demo",
                "--profile", "bdd-required",
                "--task", "订单查询 BDD 聚合",
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = valueAfter(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "bind-goal", "--project-root", "demo",
                "--scenario", "order-bound",
                "--goal", goalKey
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-bound",
                "--goal", goalKey,
                "--status", "passed",
                "--summary", "绑定场景验收通过"
        }, new Harness(tempDir).context()));

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo",
                "--goal", goalKey
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("scenario_count: 1"));
        assertTrue(verify.stdout().contains("covered_count: 1"));
        assertTrue(verify.stdout().contains("- order-bound [covered]"));
        assertTrue(!verify.stdout().contains("order-unbound"));
    }

    @Test
    void specAndWorkflowExportsIncludeBddTraceability() throws Exception {
        addOrderScenario();

        Harness spec = new Harness(tempDir);
        int specExit = new CommandRouter().run(new String[]{
                "spec", "create", "--project-root", "demo",
                "--change", "order-query-api",
                "--title", "订单查询接口",
                "--summary", "补充订单分页查询验收",
                "--module", "order",
                "--mode", "api"
        }, spec.context());
        assertEquals(ExitCodes.SUCCESS, specExit);
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "spec", "acceptance", "add", "--project-root", "demo",
                "--change", "order-query-api",
                "--acceptance", "A001",
                "--description", "分页查询返回订单列表"
        }, new Harness(tempDir).context()));

        Harness goal = new Harness(tempDir);
        int goalExit = new CommandRouter().run(new String[]{
                "goal", "start", "--project-root", "demo",
                "--profile", "java-api-change",
                "--task", "订单查询 BDD trace",
                "--module", "order"
        }, goal.context());
        assertEquals(ExitCodes.SUCCESS, goalExit);
        String runKey = valueAfter(goal.stdout(), "workflow_run: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "bind-spec", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--change", "order-query-api",
                "--acceptance", "A001"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "bind-workflow", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--run", runKey
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--status", "passed",
                "--summary", "分页查询验收通过"
        }, new Harness(tempDir).context()));

        Harness specExport = new Harness(tempDir);
        int specExportExit = new CommandRouter().run(new String[]{
                "spec", "export", "--project-root", "demo",
                "--change", "order-query-api"
        }, specExport.context());
        assertEquals(ExitCodes.SUCCESS, specExportExit);
        String specContext = new String(Files.readAllBytes(PathUtil.specContext(tempDir.resolve("demo"))), "UTF-8");
        assertTrue(specContext.contains("<bdd-trace>"));
        assertTrue(specContext.contains("A001 -> order-query-happy-path [active] 分页查询订单成功"));
        assertTrue(specContext.contains("latest_evidence=passed manual: 分页查询验收通过"));

        Harness workflowExport = new Harness(tempDir);
        int workflowExportExit = new CommandRouter().run(new String[]{
                "workflow", "export", "--project-root", "demo",
                "--run", runKey
        }, workflowExport.context());
        assertEquals(ExitCodes.SUCCESS, workflowExportExit);
        String workflowContext = new String(Files.readAllBytes(
                PathUtil.workflowContext(tempDir.resolve("demo"))), "UTF-8");
        assertTrue(workflowContext.contains("<bdd-trace>"));
        assertTrue(workflowContext.contains("order-query-happy-path [active] 分页查询订单成功"));
        assertTrue(workflowContext.contains("latest_evidence=passed manual: 分页查询验收通过"));
    }

    @Test
    void bddEvidenceVerifyAndCoverageFailWhenEvidenceIsMissingOrPending() throws Exception {
        addOrderScenario();

        Harness verifyMissing = new Harness(tempDir);
        int verifyMissingExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo", "--feature", "order-query"
        }, verifyMissing.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, verifyMissingExit);
        assertTrue(verifyMissing.stdout().contains("bdd verify failed"));
        assertTrue(verifyMissing.stdout().contains("missing_evidence: 1"));
        assertTrue(verifyMissing.stdout().contains("- order-query-happy-path [missing]"));
        assertTrue(Files.exists(PathUtil.bddEvidence(tempDir.resolve("demo"))));
        assertTrue(Files.exists(PathUtil.bddCoverage(tempDir.resolve("demo"))));

        Harness pending = new Harness(tempDir);
        int pendingExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--status", "pending",
                "--type", "manual",
                "--summary", "等待接口测试结果"
        }, pending.context());
        assertEquals(ExitCodes.SUCCESS, pendingExit);
        assertTrue(pending.stdout().contains("bdd evidence add complete"));
        assertEquals(1, countRows("bdd_evidence",
                "scenario_key = 'order-query-happy-path' AND status = 'pending'"));

        Harness verifyPending = new Harness(tempDir);
        int verifyPendingExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo", "--feature", "order-query", "--json"
        }, verifyPending.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, verifyPendingExit);
        assertTrue(verifyPending.stdout().contains("\"command\": \"bdd verify\""));
        assertTrue(verifyPending.stdout().contains("\"pending_evidence\": 1"));
        assertTrue(verifyPending.stdout().contains("\"status\": \"pending\""));

        Harness passed = new Harness(tempDir);
        int passedExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--status", "passed",
                "--type", "test",
                "--summary", "接口测试通过",
                "--command", "mvn test"
        }, passed.context());
        assertEquals(ExitCodes.SUCCESS, passedExit);
        assertTrue(passed.stdout().contains("status: passed"));

        Harness verifyPassed = new Harness(tempDir);
        int verifyPassedExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo", "--feature", "order-query"
        }, verifyPassed.context());
        assertEquals(ExitCodes.SUCCESS, verifyPassedExit);
        assertTrue(verifyPassed.stdout().contains("bdd verify passed"));
        assertTrue(verifyPassed.stdout().contains("covered_count: 1"));
        assertTrue(verifyPassed.stdout().contains("missing_evidence: 0"));
        String evidence = new String(Files.readAllBytes(PathUtil.bddEvidence(tempDir.resolve("demo"))), "UTF-8");
        assertTrue(evidence.contains("# BDD_EVIDENCE"));
        assertTrue(evidence.contains("order-query-happy-path [covered]"));
        assertTrue(evidence.contains("passed test: 接口测试通过"));

        Harness coverage = new Harness(tempDir);
        int coverageExit = new CommandRouter().run(new String[]{
                "bdd", "coverage", "--project-root", "demo", "--feature", "order-query", "--json"
        }, coverage.context());
        assertEquals(ExitCodes.SUCCESS, coverageExit);
        assertTrue(coverage.stdout().contains("\"command\": \"bdd coverage\""));
        assertTrue(coverage.stdout().contains("\"covered_count\": 1"));
        String coverageMarkdown = new String(Files.readAllBytes(
                PathUtil.bddCoverage(tempDir.resolve("demo"))), "UTF-8");
        assertTrue(coverageMarkdown.contains("# BDD_COVERAGE"));
        assertTrue(coverageMarkdown.contains("coverage_status: passed"));
        assertTrue(coverageMarkdown.contains("order-query-happy-path [covered]"));
    }

    @Test
    void bddVerifyConsumesManualEvidenceThroughAdapter() throws Exception {
        addOrderScenario();

        Harness manual = new Harness(tempDir);
        int manualExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--status", "passed",
                "--type", "manual",
                "--summary", "人工验证分页查询订单成功"
        }, manual.context());
        assertEquals(ExitCodes.SUCCESS, manualExit);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo", "--feature", "order-query", "--json"
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("\"adapter_key\": \"manual\""));
        assertTrue(verify.stdout().contains("\"adapter_normalized_status\": \"covered\""));
        assertTrue(verify.stdout().contains("\"covered\": true"));

        String evidence = new String(Files.readAllBytes(PathUtil.bddEvidence(tempDir.resolve("demo"))), "UTF-8");
        assertTrue(evidence.contains("adapter: manual source_status=passed normalized_status=covered"));
        assertTrue(evidence.contains("passed manual: 人工验证分页查询订单成功"));
    }

    @Test
    void bddManualAdapterFailureBlocksVerify() throws Exception {
        addOrderScenario();

        Harness manual = new Harness(tempDir);
        int manualExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--status", "failed",
                "--type", "manual",
                "--summary", "人工验证分页查询订单失败"
        }, manual.context());
        assertEquals(ExitCodes.SUCCESS, manualExit);

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo", "--feature", "order-query"
        }, verify.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, verifyExit);
        assertTrue(verify.stdout().contains("failed_evidence: 1"));
        assertTrue(verify.stdout().contains("- order-query-happy-path [failed]"));
        assertTrue(verify.stdout().contains("adapter: manual normalized_status=failed"));

        String evidence = new String(Files.readAllBytes(PathUtil.bddEvidence(tempDir.resolve("demo"))), "UTF-8");
        assertTrue(evidence.contains("adapter: manual source_status=failed normalized_status=failed"));
    }

    @Test
    void bddDefensiveValidationKeepsEvidenceTraceClean() throws Exception {
        addOrderScenario();
        Path root = tempDir.resolve("demo");
        writeBddRequiredProfile(root);

        Harness missingGoal = new Harness(tempDir);
        int missingGoalExit = new CommandRouter().run(new String[]{
                "bdd", "bind-goal", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--goal", "missing-goal"
        }, missingGoal.context());
        assertEquals(ExitCodes.NOT_FOUND, missingGoalExit);
        assertTrue(missingGoal.stderr().contains("error_code: GOAL_RUN_NOT_FOUND"));

        Harness missingScenario = new Harness(tempDir);
        int missingScenarioExit = new CommandRouter().run(new String[]{
                "bdd", "bind-goal", "--project-root", "demo",
                "--scenario", "missing-scenario",
                "--goal", "missing-goal"
        }, missingScenario.context());
        assertEquals(ExitCodes.NOT_FOUND, missingScenarioExit);
        assertTrue(missingScenario.stderr().contains("error_code: BDD_SCENARIO_NOT_FOUND"));

        Harness missingPath = new Harness(tempDir);
        int missingPathExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--status", "passed",
                "--evidence-path", ".agents/verification/missing.md",
                "--summary", "缺失证据路径"
        }, missingPath.context());
        assertEquals(ExitCodes.NOT_FOUND, missingPathExit);
        assertTrue(missingPath.stderr().contains("error_code: BDD_EVIDENCE_PATH_NOT_FOUND"));
        assertEquals(0, countRows("bdd_evidence", "scenario_key = 'order-query-happy-path'"));

        write(root, ".agents/verification/地址验收.md", "人工验收记录\n");
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--status", "failed",
                "--evidence-path", ".agents/verification/地址验收.md",
                "--summary", "首次验收失败"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--status", "passed",
                "--evidence-path", ".agents/verification/地址验收.md",
                "--summary", "修复后验收通过"
        }, new Harness(tempDir).context()));

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo",
                "--scenario", "order-query-happy-path"
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("- order-query-happy-path [covered]"));
        String evidence = new String(Files.readAllBytes(PathUtil.bddEvidence(root)), "UTF-8");
        assertTrue(evidence.contains("修复后验收通过"));
        assertTrue(evidence.contains("首次验收失败"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start", "--project-root", "demo",
                "--profile", "bdd-required",
                "--task", "完成后拒绝追加 BDD evidence",
                "--module", "order",
                "--force-new"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = valueAfter(start.stdout(), "goal_key: ");
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "bind-goal", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--goal", goalKey
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--goal", goalKey,
                "--status", "passed",
                "--summary", "goal scoped evidence passed"
        }, new Harness(tempDir).context()));
        recordGoalStep(goalKey, "Inspected BDD behavior",
                "existing_controller=bdd scenario",
                "existing_service=bdd verification",
                "existing_mapper=bdd repositories",
                "existing_tests=bdd defensive test");
        recordGoalStep(goalKey, "Verified BDD behavior",
                "compile_result=not required for fixture",
                "test_result=bdd defensive fixture",
                "sensitive_result=passed");
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo",
                "--goal", goalKey,
                "--all"
        }, new Harness(tempDir).context()));
        Harness complete = new Harness(tempDir);
        int completeExit = new CommandRouter().run(new String[]{
                "goal", "complete", "--project-root", "demo",
                "--goal", goalKey
        }, complete.context());
        assertEquals(ExitCodes.SUCCESS, completeExit);
        assertTrue(complete.stdout().contains("status: completed"));

        Harness lateEvidence = new Harness(tempDir);
        int lateEvidenceExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--goal", goalKey,
                "--status", "passed",
                "--summary", "late evidence"
        }, lateEvidence.context());
        assertEquals(ExitCodes.USAGE_ERROR, lateEvidenceExit);
        assertTrue(lateEvidence.stderr().contains("error_code: BDD_EVIDENCE_GOAL_COMPLETED"));
    }

    @Test
    void bddJunitEvidenceAdapterImportsSurefireResultsAndVerifySeesFailures() throws Exception {
        addOrderScenario();
        addScenario("order-query-failure", "分页查询订单失败", "系统存在异常订单", "用户查询订单", "接口返回错误");
        addScenario("order-query-skipped", "分页查询订单跳过", "系统存在订单", "用户查询订单", "测试暂不可用");

        bindTest("order-query-happy-path", "com.example.OrderControllerTest", "querySuccess");
        bindTest("order-query-failure", "com.example.OrderControllerTest", "queryFailure");
        bindTest("order-query-skipped", "com.example.OrderControllerTest", "querySkipped");

        Path root = tempDir.resolve("demo");
        write(root, "target/surefire-reports/TEST-com.example.OrderControllerTest.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<testsuite name=\"com.example.OrderControllerTest\" tests=\"3\" failures=\"1\" skipped=\"1\">\n"
                        + "  <testcase classname=\"com.example.OrderControllerTest\" name=\"querySuccess\"/>\n"
                        + "  <testcase classname=\"com.example.OrderControllerTest\" name=\"queryFailure\">\n"
                        + "    <failure message=\"assertion failed\"/>\n"
                        + "  </testcase>\n"
                        + "  <testcase classname=\"com.example.OrderControllerTest\" name=\"querySkipped\">\n"
                        + "    <skipped/>\n"
                        + "  </testcase>\n"
                        + "</testsuite>\n");

        Harness junit = new Harness(tempDir);
        int junitExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "junit", "--project-root", "demo",
                "--reports", "target/surefire-reports"
        }, junit.context());
        assertEquals(ExitCodes.SUCCESS, junitExit);
        assertTrue(junit.stdout().contains("bdd evidence junit complete"));
        assertTrue(junit.stdout().contains("passed_count: 1"));
        assertTrue(junit.stdout().contains("failed_count: 1"));
        assertTrue(junit.stdout().contains("skipped_count: 1"));
        assertEquals(1, countRows("bdd_evidence",
                "scenario_key = 'order-query-failure' AND status = 'failed' "
                        + "AND command = 'junit:com.example.OrderControllerTest#queryFailure'"));

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo", "--feature", "order-query"
        }, verify.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, verifyExit);
        assertTrue(verify.stdout().contains("failed_evidence: 1"));
        assertTrue(verify.stdout().contains("- order-query-failure [failed]"));
        assertTrue(verify.stdout().contains("adapter: junit normalized_status=failed"));

        String evidence = new String(Files.readAllBytes(PathUtil.bddEvidence(root)), "UTF-8");
        assertTrue(evidence.contains("adapter: junit source_status=failed normalized_status=failed"));
        assertTrue(evidence.contains("failed test: JUnit result failed for com.example.OrderControllerTest#queryFailure"));
    }

    @Test
    void bddJunitEvidenceAdapterRecordsPendingWhenReportIsMissing() throws Exception {
        addOrderScenario();
        bindTest("order-query-happy-path", "com.example.OrderControllerTest", "querySuccess");

        Harness junit = new Harness(tempDir);
        int junitExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "junit", "--project-root", "demo",
                "--reports", "target/missing-surefire-reports",
                "--scenario", "order-query-happy-path"
        }, junit.context());
        assertEquals(ExitCodes.SUCCESS, junitExit);
        assertTrue(junit.stdout().contains("binding_count: 1"));
        assertTrue(junit.stdout().contains("pending_count: 1"));
        assertEquals(1, countRows("bdd_evidence",
                "scenario_key = 'order-query-happy-path' AND status = 'pending' "
                        + "AND command = 'junit:com.example.OrderControllerTest#querySuccess'"));

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo", "--feature", "order-query"
        }, verify.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, verifyExit);
        assertTrue(verify.stdout().contains("pending_evidence: 1"));
        assertTrue(verify.stdout().contains("adapter: junit normalized_status=pending"));

        String evidence = new String(Files.readAllBytes(PathUtil.bddEvidence(tempDir.resolve("demo"))), "UTF-8");
        assertTrue(evidence.contains("adapter: junit source_status=pending normalized_status=pending"));
        assertTrue(evidence.contains("pending test: JUnit result missing for com.example.OrderControllerTest#querySuccess"));
    }

    @Test
    void bddOptionalJsonReportAdaptersImportExternalAcceptanceResults() throws Exception {
        addScenario("order-query-cucumber", "Cucumber 验收通过", "已有订单", "执行 Cucumber 场景", "场景通过");
        addScenario("order-query-postman", "Postman 验收失败", "已有订单", "执行 Newman 集合", "集合失败");
        addScenario("order-query-playwright", "Playwright 验收跳过", "已有订单", "执行页面测试", "测试跳过");
        Path root = tempDir.resolve("demo");
        write(root, "target/cucumber.json",
                "[{\"elements\":[{\"name\":\"order-query-cucumber\",\"steps\":[{\"result\":{\"status\":\"passed\"}}]}]}]");
        write(root, "target/newman/report.json",
                "{\"run\":{\"stats\":{\"assertions\":{\"failed\":1}},\"failures\":[{\"source\":{\"name\":\"order-query-postman\"}}]}}");
        write(root, "target/playwright/results.json",
                "{\"suites\":[{\"specs\":[{\"title\":\"order-query-playwright\",\"tests\":[{\"results\":[{\"status\":\"skipped\"}]}]}]}]}");

        importReport("cucumber", "target/cucumber.json", "order-query-cucumber", "passed");
        importReport("postman", "target/newman/report.json", "order-query-postman", "failed");
        importReport("playwright", "target/playwright/results.json", "order-query-playwright", "skipped");

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo", "--feature", "order-query"
        }, verify.context());
        assertEquals(ExitCodes.VALIDATION_ERROR, verifyExit);
        assertTrue(verify.stdout().contains("failed_evidence: 1"));
        assertTrue(verify.stdout().contains("- order-query-postman [failed]"));
        assertTrue(verify.stdout().contains("adapter: postman normalized_status=failed"));

        String evidence = new String(Files.readAllBytes(PathUtil.bddEvidence(root)), "UTF-8");
        assertTrue(evidence.contains("adapter: cucumber source_status=passed normalized_status=covered"));
        assertTrue(evidence.contains("adapter: postman source_status=failed normalized_status=failed"));
        assertTrue(evidence.contains("adapter: playwright source_status=skipped normalized_status=covered"));
    }

    @Test
    void goalVerifyRunsBddRequiredCheckForGoalBoundScenarios() throws Exception {
        addOrderScenario();
        Path root = tempDir.resolve("demo");
        writeBddRequiredProfile(root);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start", "--project-root", "demo",
                "--profile", "bdd-required",
                "--task", "订单查询 BDD 验收",
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = valueAfter(start.stdout(), "goal_key: ");
        String goalContext = new String(Files.readAllBytes(PathUtil.goalContext(root)), "UTF-8");
        assertTrue(goalContext.contains("- bdd"));
        assertTrue(goalContext.contains("<bdd-status>"));
        assertTrue(goalContext.contains("- bdd_required: true"));
        assertTrue(goalContext.contains("- bound_scenario_count: 0"));

        Harness missingBinding = new Harness(tempDir);
        int missingBindingExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo",
                "--goal", goalKey, "--check", "bdd"
        }, missingBinding.context());
        assertEquals(ExitCodes.SUCCESS, missingBindingExit);
        assertTrue(missingBinding.stdout().contains("check_key: bdd"));
        assertTrue(missingBinding.stdout().contains("status: failed"));
        assertTrue(missingBinding.stdout().contains("no scenarios are bound to goal"));

        Harness bindGoal = new Harness(tempDir);
        int bindGoalExit = new CommandRouter().run(new String[]{
                "bdd", "bind-goal", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--goal", goalKey
        }, bindGoal.context());
        assertEquals(ExitCodes.SUCCESS, bindGoalExit);

        Harness pending = new Harness(tempDir);
        int pendingExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--goal", goalKey,
                "--status", "pending",
                "--summary", "等待验收证据"
        }, pending.context());
        assertEquals(ExitCodes.SUCCESS, pendingExit);

        Harness verifyPending = new Harness(tempDir);
        int verifyPendingExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo",
                "--goal", goalKey
        }, verifyPending.context());
        assertEquals(ExitCodes.SUCCESS, verifyPendingExit);
        assertTrue(verifyPending.stdout().contains("bdd: failed"));
        assertTrue(verifyPending.stdout().contains("scenario order-query-happy-path evidence is pending"));
        assertTrue(verifyPending.stdout().contains("ready_to_complete: false"));

        Harness passed = new Harness(tempDir);
        int passedExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--goal", goalKey,
                "--status", "passed",
                "--summary", "验收证据通过"
        }, passed.context());
        assertEquals(ExitCodes.SUCCESS, passedExit);

        Harness checkPassed = new Harness(tempDir);
        int checkPassedExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo",
                "--goal", goalKey, "--check", "bdd"
        }, checkPassed.context());
        assertEquals(ExitCodes.SUCCESS, checkPassedExit);
        assertTrue(checkPassed.stdout().contains("check_key: bdd"));
        assertTrue(checkPassed.stdout().contains("status: passed"));
        assertTrue(checkPassed.stdout().contains("bdd scenarios covered"));

        Files.createDirectories(PathUtil.bddExportsDirectory(root));
        Files.write(PathUtil.scenarioImpactMap(root),
                "# SCENARIO_IMPACT_MAP\n\n<summary>\n- scenario_key: order-query-happy-path\n</summary>\n"
                        .getBytes("UTF-8"));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "goal", "export", "--project-root", "demo",
                "--goal", goalKey
        }, export.context());
        assertEquals(ExitCodes.SUCCESS, exportExit);
        String updatedContext = new String(Files.readAllBytes(PathUtil.goalContext(root)), "UTF-8");
        assertTrue(updatedContext.contains("<bdd-status>"));
        assertTrue(updatedContext.contains("- bound_scenario_count: 1"));
        assertTrue(updatedContext.contains("- covered_count: 1"));
        assertTrue(updatedContext.contains(".agents/bdd/exports/BDD_EVIDENCE.md"));
        assertTrue(updatedContext.contains(".agents/bdd/exports/SCENARIO_IMPACT_MAP.md"));
        assertTrue(updatedContext.contains("- scenario_impact_map_exists: true"));
    }

    @Test
    void goalBddCheckCanFailOnConfiguredQualityScoreGate() throws Exception {
        addScenario("order-quality-gate", "测试场景", "准备数据", "用户操作", "系统正常");
        Path root = tempDir.resolve("demo");
        writeBddRequiredProfile(root);
        Files.createDirectories(PathUtil.devharnessDirectory(root));
        Files.write(PathUtil.goalCheckPolicy(root),
                ("{\n"
                        + "  \"bdd_min_quality_score\": \"100\",\n"
                        + "  \"bdd_fail_on_quality_errors\": \"true\"\n"
                        + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start", "--project-root", "demo",
                "--profile", "bdd-required",
                "--task", "订单查询 BDD 质量门禁",
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = valueAfter(start.stdout(), "goal_key: ");

        Harness bindGoal = new Harness(tempDir);
        int bindGoalExit = new CommandRouter().run(new String[]{
                "bdd", "bind-goal", "--project-root", "demo",
                "--scenario", "order-quality-gate",
                "--goal", goalKey
        }, bindGoal.context());
        assertEquals(ExitCodes.SUCCESS, bindGoalExit);

        Harness evidence = new Harness(tempDir);
        int evidenceExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-quality-gate",
                "--goal", goalKey,
                "--status", "passed",
                "--summary", "验收证据通过"
        }, evidence.context());
        assertEquals(ExitCodes.SUCCESS, evidenceExit);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo",
                "--goal", goalKey, "--check", "bdd"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: bdd"));
        assertTrue(check.stdout().contains("status: failed"));
        assertTrue(check.stdout().contains("bdd quality score"));
        assertTrue(check.stdout().contains("below threshold 100"));
    }

    @Test
    void goalBddCheckCanFailOnConfiguredCoverageThreshold() throws Exception {
        addScenario("order-covered", "分页查询订单成功",
                "存在三笔订单", "用户按分页条件查询", "返回分页订单结果");
        addScenario("order-uncovered", "风险订单需要复核",
                "存在一笔高风险订单", "用户提交复核", "记录复核结果");
        Path root = tempDir.resolve("demo");
        writeBddRequiredProfile(root);
        Files.createDirectories(PathUtil.devharnessDirectory(root));
        Files.write(PathUtil.goalCheckPolicy(root),
                ("{\n"
                        + "  \"bdd_min_coverage_percent\": \"100\"\n"
                        + "}\n").getBytes("UTF-8"));

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start", "--project-root", "demo",
                "--profile", "bdd-required",
                "--task", "订单查询 BDD 覆盖率门禁",
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = valueAfter(start.stdout(), "goal_key: ");

        for (String scenario : new String[]{"order-covered", "order-uncovered"}) {
            Harness bindGoal = new Harness(tempDir);
            int bindGoalExit = new CommandRouter().run(new String[]{
                    "bdd", "bind-goal", "--project-root", "demo",
                    "--scenario", scenario,
                    "--goal", goalKey
            }, bindGoal.context());
            assertEquals(ExitCodes.SUCCESS, bindGoalExit);
        }

        Harness evidence = new Harness(tempDir);
        int evidenceExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-covered",
                "--goal", goalKey,
                "--status", "passed",
                "--summary", "分页查询验收证据通过"
        }, evidence.context());
        assertEquals(ExitCodes.SUCCESS, evidenceExit);

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo",
                "--goal", goalKey, "--check", "bdd"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: bdd"));
        assertTrue(check.stdout().contains("status: failed"));
        assertTrue(check.stdout().contains("scenario order-uncovered evidence is missing"));
        assertTrue(check.stdout().contains("bdd coverage 50% is below threshold 100%"));
    }

    @Test
    void bddAndGraphRequiredGoalRequiresScenarioImpactMap() throws Exception {
        addOrderScenario();
        Path root = tempDir.resolve("demo");
        write(root, "src/main/java/com/example/OrderController.java",
                "package com.example;\npublic class OrderController { public void query() {} }\n");
        writeBddGraphRequiredProfile(root);

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start", "--project-root", "demo",
                "--profile", "bdd-graph-required",
                "--task", "订单查询 BDD + Graph 验收",
                "--module", "order"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = valueAfter(start.stdout(), "goal_key: ");

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "bind-goal", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--goal", goalKey
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "evidence", "add", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--goal", goalKey,
                "--status", "passed",
                "--summary", "BDD 验收证据通过"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "index", "--project-root", "demo"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "export", "--project-root", "demo"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "demo",
                "--file", "src/main/java/com/example/OrderController.java"
        }, new Harness(tempDir).context()));

        Harness next = new Harness(tempDir);
        int nextExit = new CommandRouter().run(new String[]{
                "goal", "next", "--project-root", "demo", "--goal", goalKey
        }, next.context());
        assertEquals(ExitCodes.SUCCESS, nextExit);
        assertTrue(next.stdout().contains("scenario_impact:"));
        assertTrue(next.stdout().contains("exists: false"));
        assertTrue(next.stdout().contains("graph-impact.sh --project-root"));
        assertTrue(next.stdout().contains("--scenario <scenario-key>"));

        Harness verifyMissing = new Harness(tempDir);
        int verifyMissingExit = new CommandRouter().run(new String[]{
                "goal", "verify", "--project-root", "demo", "--goal", goalKey
        }, verifyMissing.context());
        assertEquals(ExitCodes.SUCCESS, verifyMissingExit);
        assertTrue(verifyMissing.stdout().contains("impact: failed"));
        assertTrue(verifyMissing.stdout().contains("SCENARIO_IMPACT_MAP.md missing"));
        assertTrue(verifyMissing.stdout().contains("ready_to_complete: false"));

        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "bdd", "bind-graph", "--project-root", "demo",
                "--scenario", "order-query-happy-path",
                "--file", "src/main/java/com/example/OrderController.java"
        }, new Harness(tempDir).context()));
        assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                "graph", "impact", "--project-root", "demo",
                "--scenario", "order-query-happy-path"
        }, new Harness(tempDir).context()));

        Harness impactCheck = new Harness(tempDir);
        int impactCheckExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo",
                "--goal", goalKey, "--check", "impact"
        }, impactCheck.context());
        assertEquals(ExitCodes.SUCCESS, impactCheckExit);
        assertTrue(impactCheck.stdout().contains("check_key: impact"));
        assertTrue(impactCheck.stdout().contains("status: passed"));
    }

    @Test
    void addressDeliveryValidationEndToEndBddAcceptancePassesGoalCheck() throws Exception {
        Path root = tempDir.resolve("demo");
        writeBddRequiredProfile(root);
        String[][] scenarios = new String[][]{
                {"address-required-fields", "必填字段缺失时提示错误", "用户进入新增配送地址页面", "用户未填写收件人或手机号直接提交", "系统提示必填字段错误"},
                {"address-phone-format", "手机号格式错误时提示错误", "用户填写错误手机号", "用户提交新增配送地址", "系统提示手机号格式错误"},
                {"address-save-success", "地址保存成功后出现在地址列表", "用户填写完整配送地址", "用户保存地址", "地址列表展示新地址"},
                {"address-single-default", "默认地址只能有一个", "用户已有一个默认地址", "用户将另一个地址设为默认", "旧默认地址自动取消默认"},
                {"address-delete-refresh", "删除地址后列表刷新", "用户已有配送地址", "用户删除其中一个地址", "地址列表不再展示该地址"}
        };
        for (String[] scenario : scenarios) {
            Harness create = new Harness(tempDir);
            int createExit = new CommandRouter().run(new String[]{
                    "bdd", "scenario", "create", "--project-root", "demo",
                    "--module", "address",
                    "--scenario", scenario[0],
                    "--title", scenario[1],
                    "--given", scenario[2],
                    "--when", scenario[3],
                    "--then", scenario[4]
            }, create.context());
            assertEquals(ExitCodes.SUCCESS, createExit);
        }

        Harness start = new Harness(tempDir);
        int startExit = new CommandRouter().run(new String[]{
                "goal", "start", "--project-root", "demo",
                "--profile", "bdd-required",
                "--task", "新增配送地址校验",
                "--module", "address"
        }, start.context());
        assertEquals(ExitCodes.SUCCESS, startExit);
        String goalKey = valueAfter(start.stdout(), "goal_key: ");

        for (String[] scenario : scenarios) {
            assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                    "bdd", "bind-goal", "--project-root", "demo",
                    "--scenario", scenario[0],
                    "--goal", goalKey
            }, new Harness(tempDir).context()));
            assertEquals(ExitCodes.SUCCESS, new CommandRouter().run(new String[]{
                    "bdd", "evidence", "add", "--project-root", "demo",
                    "--scenario", scenario[0],
                    "--goal", goalKey,
                    "--status", "passed",
                    "--summary", "手动验收通过: " + scenario[1]
            }, new Harness(tempDir).context()));
        }

        Harness verify = new Harness(tempDir);
        int verifyExit = new CommandRouter().run(new String[]{
                "bdd", "verify", "--project-root", "demo",
                "--goal", goalKey
        }, verify.context());
        assertEquals(ExitCodes.SUCCESS, verifyExit);
        assertTrue(verify.stdout().contains("scenario_count: 5"));
        assertTrue(verify.stdout().contains("covered_count: 5"));
        assertTrue(verify.stdout().contains("missing_evidence: 0"));

        Harness coverage = new Harness(tempDir);
        int coverageExit = new CommandRouter().run(new String[]{
                "bdd", "coverage", "--project-root", "demo",
                "--goal", goalKey
        }, coverage.context());
        assertEquals(ExitCodes.SUCCESS, coverageExit);
        assertTrue(coverage.stdout().contains("covered_count: 5"));

        Harness check = new Harness(tempDir);
        int checkExit = new CommandRouter().run(new String[]{
                "goal", "check", "--project-root", "demo",
                "--goal", goalKey,
                "--check", "bdd"
        }, check.context());
        assertEquals(ExitCodes.SUCCESS, checkExit);
        assertTrue(check.stdout().contains("check_key: bdd"));
        assertTrue(check.stdout().contains("status: passed"));
        assertTrue(check.stdout().contains("bound_scenarios=5"));

        Harness export = new Harness(tempDir);
        int exportExit = new CommandRouter().run(new String[]{
                "goal", "export", "--project-root", "demo",
                "--goal", goalKey
        }, export.context());
        assertEquals(ExitCodes.SUCCESS, exportExit);
        String goalContext = new String(Files.readAllBytes(PathUtil.goalContext(root)), "UTF-8");
        assertTrue(goalContext.contains("- bound_scenario_count: 5"));
        assertTrue(goalContext.contains("- covered_count: 5"));
        String evidence = new String(Files.readAllBytes(PathUtil.bddEvidence(root)), "UTF-8");
        assertTrue(evidence.contains("address-required-fields [covered]"));
        assertTrue(evidence.contains("address-single-default [covered]"));
    }

    private int countRows(String tableName, String where) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + PathUtil.memoryDb(tempDir.resolve("demo")).toString());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + tableName + " WHERE " + where)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void addOrderScenario() {
        addScenario("order-query-happy-path", "分页查询订单成功",
                "已有订单数据", "用户按分页条件查询", "返回订单分页结果");
    }

    private void addScenario(String scenarioKey, String scenarioTitle,
                             String given, String when, String then) {
        Harness add = new Harness(tempDir);
        int addExit = new CommandRouter().run(new String[]{
                "bdd", "add", "--project-root", "demo",
                "--feature", "order-query",
                "--title", "订单查询",
                "--scenario", scenarioKey,
                "--scenario-title", scenarioTitle,
                "--module", "order",
                "--tags", "api,order",
                "--given", given,
                "--when", when,
                "--then", then
        }, add.context());
        assertEquals(ExitCodes.SUCCESS, addExit);
    }

    private void bindTest(String scenarioKey, String className, String methodName) {
        Harness bind = new Harness(tempDir);
        int bindExit = new CommandRouter().run(new String[]{
                "bdd", "bind-test", "--project-root", "demo",
                "--scenario", scenarioKey,
                "--class", className,
                "--method", methodName
        }, bind.context());
        assertEquals(ExitCodes.SUCCESS, bindExit);
        assertTrue(bind.stdout().contains("bdd bind-test complete"));
    }

    private void recordGoalStep(String goalKey, String summary, String... fields) {
        Harness step = new Harness(tempDir);
        java.util.List<String> raw = new java.util.ArrayList<String>();
        raw.add("goal");
        raw.add("step");
        raw.add("--project-root");
        raw.add("demo");
        raw.add("--goal");
        raw.add(goalKey);
        raw.add("--summary");
        raw.add(summary);
        for (String field : fields) {
            raw.add("--field");
            raw.add(field);
        }
        int stepExit = new CommandRouter().run(raw.toArray(new String[raw.size()]), step.context());
        assertEquals(ExitCodes.SUCCESS, stepExit, "stdout=" + step.stdout() + "\nstderr=" + step.stderr());
    }

    private void importReport(String adapterKey, String reportPath, String scenarioKey, String expectedStatus) {
        Harness report = new Harness(tempDir);
        int reportExit = new CommandRouter().run(new String[]{
                "bdd", "evidence", "report", "--project-root", "demo",
                "--adapter", adapterKey,
                "--report", reportPath,
                "--scenario", scenarioKey
        }, report.context());
        assertEquals(ExitCodes.SUCCESS, reportExit);
        assertTrue(report.stdout().contains("bdd evidence report complete"));
        assertTrue(report.stdout().contains("adapter_key: " + adapterKey));
        assertTrue(report.stdout().contains("status: " + expectedStatus));
    }

    private void writeBddRequiredProfile(Path root) throws Exception {
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        String json = "{\n"
                + "  \"profile_key\": \"bdd-required\",\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect_existing_code,verify\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"required_checks\": \"bdd\",\n"
                + "  \"completion_allow_skipped_checks\": \"false\",\n"
                + "  \"completion_require_checkpoint\": \"false\",\n"
                + "  \"bdd_required\": \"true\"\n"
                + "}\n";
        Files.write(PathUtil.goalProfile(root, "bdd-required"), json.getBytes("UTF-8"));
    }

    private void writeBddGraphRequiredProfile(Path root) throws Exception {
        Files.createDirectories(PathUtil.goalProfilesDirectory(root));
        String json = "{\n"
                + "  \"profile_key\": \"bdd-graph-required\",\n"
                + "  \"workflow_key\": \"api-change\",\n"
                + "  \"default_mode\": \"api\",\n"
                + "  \"actions\": \"inspect_existing_code,verify\",\n"
                + "  \"requires_spec\": \"false\",\n"
                + "  \"required_checks\": \"graph,impact,bdd\",\n"
                + "  \"completion_allow_skipped_checks\": \"false\",\n"
                + "  \"completion_require_checkpoint\": \"false\",\n"
                + "  \"bdd_required\": \"true\",\n"
                + "  \"graph_required\": \"true\",\n"
                + "  \"graph_require_fresh_snapshot\": \"true\",\n"
                + "  \"graph_require_impact_map\": \"true\"\n"
                + "}\n";
        Files.write(PathUtil.goalProfile(root, "bdd-graph-required"), json.getBytes("UTF-8"));
    }

    private void write(Path root, String relativePath, String content) throws Exception {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes("UTF-8"));
    }

    private String valueAfter(String text, String prefix) {
        String[] lines = text.split("\\R");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private static final class Harness {
        private final Path workingDirectory;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        private Harness(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        private CommandContext context() {
            return new CommandContext(
                    workingDirectory.toAbsolutePath().normalize(),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(out),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(err),
                    new FixedClock()
            );
        }

        private String stdout() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(out);
        }

        private String stderr() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(err);
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
