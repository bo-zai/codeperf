package com.cmb.codeperf.server.model.vo.report;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分支报告中的风险负责人分组。
 */
@Data
public class BranchReportOwnerGroupVO {

    private String ownerName;
    private String ownerEmail;
    private int issueCount;
    private int corroboratedCount;
    private List<BranchReportIssueVO> issues = new ArrayList<>();
}
