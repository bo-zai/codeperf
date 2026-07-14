package com.codeperf.server.application.service;

import com.codeperf.server.api.dto.RiskAttributionSummary;
import com.codeperf.server.api.dto.StaticFindingSummary;
import com.codeperf.server.api.dto.StaticReportSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class StaticReportSummarizer {

    private final ObjectMapper mapper = new ObjectMapper();

    public void validate(String payload) {
        parse(payload);
    }

    public StaticReportSummary summarize(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        JsonNode root = parse(payload);
        JsonNode findings = root.path("findings");
        JsonNode parseErrors = root.path("parseErrors");
        return new StaticReportSummary(
                root.path("filesScanned").asInt(0),
                findings.isArray() ? findings.size() : 0,
                parseErrors.isArray() ? parseErrors.size() : 0,
                summarizeFindings(findings));
    }

    private List<StaticFindingSummary> summarizeFindings(JsonNode findings) {
        List<StaticFindingSummary> summaries = new ArrayList<>();
        if (!findings.isArray()) {
            return summaries;
        }
        for (JsonNode finding : findings) {
            summaries.add(new StaticFindingSummary(
                    text(finding, "type"),
                    text(finding, "severity"),
                    text(finding, "confidence"),
                    text(finding, "sourceFile"),
                    finding.path("lineNumber").asInt(0),
                    finding.path("loopStartLine").asInt(0),
                    finding.path("loopEndLine").asInt(0),
                    text(finding, "ioType"),
                    text(finding, "loopMethodName"),
                    finding.path("loopCallLine").asInt(0),
                    finding.path("ioLine").asInt(0),
                    summarizeAttribution(finding.path("attribution"))));
        }
        return summaries;
    }

    private RiskAttributionSummary summarizeAttribution(JsonNode attribution) {
        if (attribution.isMissingNode() || attribution.isNull()) {
            return null;
        }
        return new RiskAttributionSummary(
                text(attribution, "riskScope"),
                attribution.path("changedLine").asBoolean(false),
                text(attribution, "attributionConfidence"),
                text(attribution, "introducedByName"),
                text(attribution, "introducedByEmail"),
                text(attribution, "introducedCommit"),
                text(attribution, "introducedCommitTime"),
                text(attribution, "introducedCommitMessage"));
    }

    private JsonNode parse(String payload) {
        try {
            return mapper.readTree(payload);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid static result json", e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }
}
