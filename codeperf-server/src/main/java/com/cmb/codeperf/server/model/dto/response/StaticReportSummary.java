package com.cmb.codeperf.server.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaticReportSummary {
    private int filesScanned;
    private int findingCount;
    private int parseErrorCount;
    private List<StaticFindingSummary> findings = new ArrayList<>();
}

