package com.devharnesskit.dhk.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EvidenceValueParserTest {
    @Test
    void readsPlainKeyValueEvidence() {
        String evidence = "compile_result=passed; test_scope=EvidenceValueParserTest\n"
                + "manual_evidence_status=passed";

        assertEquals("passed", EvidenceValueParser.value(evidence, "compile_result"));
        assertEquals("EvidenceValueParserTest", EvidenceValueParser.value(evidence, "test_scope"));
        assertEquals("passed", EvidenceValueParser.value(evidence, "manual_evidence_status"));
    }

    @Test
    void readsFieldArgumentsEmbeddedInEvidenceText() {
        String evidence = "compile_result=passed; --field manual_evidence_status=passed "
                + "--field tester=codex";

        assertEquals("passed", EvidenceValueParser.value(evidence, "manual_evidence_status"));
        assertEquals("codex", EvidenceValueParser.value(evidence, "tester"));
    }

    @Test
    void doesNotSplitPlainValuesThatContainFieldLikeText() {
        String evidence = "scope_justification=We set --field inject=false for safety; "
                + "--field tester=developer";

        assertEquals("We set --field inject=false for safety",
                EvidenceValueParser.value(evidence, "scope_justification"));
        assertEquals("", EvidenceValueParser.value(evidence, "inject"));
        assertEquals("developer", EvidenceValueParser.value(evidence, "tester"));
    }

    @Test
    void readsQuotedAndEscapedFieldValues() {
        String evidence = "--field summary=\"say \\\"hello\\\" and \\\\ path\" "
                + "--field owner='team lead'";

        assertEquals("say \"hello\" and \\ path", EvidenceValueParser.value(evidence, "summary"));
        assertEquals("team lead", EvidenceValueParser.value(evidence, "owner"));
    }

    @Test
    void keepsQuotedValuesThatContainFieldLikeText() {
        String evidence = "--field scope_justification=\"We set --field inject=false\" "
                + "--field tester=developer";

        assertEquals("We set --field inject=false",
                EvidenceValueParser.value(evidence, "scope_justification"));
        assertEquals("", EvidenceValueParser.value(evidence, "inject"));
        assertEquals("developer", EvidenceValueParser.value(evidence, "tester"));
    }

    @Test
    void returnsLastOccurrenceForRepeatedKey() {
        String plainEvidence = "manual_evidence_status=failed; manual_evidence_status=passed";
        String mixedEvidence = "manual_evidence_status=failed; "
                + "--field manual_evidence_status=passed; manual_evidence_status=stale";

        assertEquals("passed", EvidenceValueParser.value(plainEvidence, "manual_evidence_status"));
        assertEquals("stale", EvidenceValueParser.value(mixedEvidence, "manual_evidence_status"));
    }

    @Test
    void returnsEmptyForNullOrEmptyInput() {
        assertEquals("", EvidenceValueParser.value(null, "manual_evidence_status"));
        assertEquals("", EvidenceValueParser.value("", "manual_evidence_status"));
        assertEquals("", EvidenceValueParser.value("manual_evidence_status=passed", null));
        assertEquals("", EvidenceValueParser.value("manual_evidence_status=passed", ""));
        assertEquals("", EvidenceValueParser.value("manual_evidence_status=passed", "  "));
    }
}
