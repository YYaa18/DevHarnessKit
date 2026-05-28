package com.devharnesskit.dhk.service.brief;

import com.devharnesskit.dhk.model.brief.GrowthLesson;
import com.devharnesskit.dhk.model.brief.InteractionRequest;
import com.devharnesskit.dhk.model.brief.KnowledgeCandidate;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class BriefLifecycleCompatibilityStoreWriter {
    void writeInteractions(Path projectRoot, List<InteractionRequest> requests) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        for (InteractionRequest request : requests) {
            builder.append(escape(request.requestId())).append('\t')
                    .append(escape(request.goalKey())).append('\t')
                    .append(escape(request.phase())).append('\t')
                    .append(escape(request.type())).append('\t')
                    .append(escape(request.priority())).append('\t')
                    .append(escape(request.question())).append('\t')
                    .append(escape(request.why())).append('\t')
                    .append(escape(request.choices())).append('\t')
                    .append(escape(request.defaultChoice())).append('\t')
                    .append(request.blocksProgress()).append('\t')
                    .append(escape(request.status())).append('\t')
                    .append(escape(request.answer())).append('\n');
        }
        Files.write(PathUtil.interactionRequests(projectRoot), builder.toString().getBytes("UTF-8"));
    }

    void writeCandidates(Path projectRoot, List<KnowledgeCandidate> candidates) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        for (KnowledgeCandidate candidate : candidates) {
            builder.append(escape(candidate.candidateId())).append('\t')
                    .append(escape(candidate.goalKey())).append('\t')
                    .append(escape(candidate.type())).append('\t')
                    .append(escape(candidate.title())).append('\t')
                    .append(escape(candidate.summary())).append('\t')
                    .append(escape(candidate.evidenceRefs())).append('\t')
                    .append(escape(candidate.suggestedDestination())).append('\t')
                    .append(escape(candidate.confidence())).append('\t')
                    .append(candidate.requiresConfirmation()).append('\t')
                    .append(escape(candidate.sensitiveScanStatus())).append('\t')
                    .append(escape(candidate.status())).append('\n');
        }
        Files.write(PathUtil.knowledgeCandidatesStore(projectRoot), builder.toString().getBytes("UTF-8"));
    }

    void writeGrowthLessons(Path projectRoot, List<GrowthLesson> lessons) throws Exception {
        Files.createDirectories(PathUtil.devharnessBriefsDirectory(projectRoot));
        StringBuilder builder = new StringBuilder();
        for (GrowthLesson lesson : lessons) {
            builder.append(escape(lesson.lessonId())).append('\t')
                    .append(escape(lesson.sourceCandidateId())).append('\t')
                    .append(escape(lesson.title())).append('\t')
                    .append(escape(lesson.summary())).append('\t')
                    .append(escape(lesson.status())).append('\t')
                    .append(lesson.advisoryOnly()).append('\n');
        }
        Files.write(PathUtil.growthLessonsStore(projectRoot), builder.toString().getBytes("UTF-8"));
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }
}
