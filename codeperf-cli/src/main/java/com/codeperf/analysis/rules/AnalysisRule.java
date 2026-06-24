package com.codeperf.analysis.rules;

import com.codeperf.analysis.Finding;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 分析规则接口。每条规则从 SessionData JSON 中提取问题。
 */
public interface AnalysisRule {
    List<Finding> analyze(JsonNode session);
}
