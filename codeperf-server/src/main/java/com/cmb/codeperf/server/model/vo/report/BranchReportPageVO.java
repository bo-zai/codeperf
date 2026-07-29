package com.cmb.codeperf.server.model.vo.report;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分支综合报告页面模型。
 * 该页面面向公共分支流水线场景，风险按真实引入人聚合，而不是按流水线执行人聚合。
 */
@Data
public class BranchReportPageVO {

    private String remoteUrl;
    private String branch;
    private String env;
    private String latestDynamicTaskId;
    private int openIssueCount;
    private int corroboratedIssueCount;
    private int ownerCount;
    private List<BranchReportOwnerGroupVO> ownerGroups = new ArrayList<>();
}
