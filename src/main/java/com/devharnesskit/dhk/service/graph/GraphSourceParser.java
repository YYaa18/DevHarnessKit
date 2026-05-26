package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphParseResult;

import java.nio.file.Path;

interface GraphSourceParser {
    boolean supports(GraphFileEntry entry);

    void parse(Path projectRoot, GraphFileEntry entry, GraphParseResult.Builder builder) throws Exception;
}
