package com.cmb.codeperf.server.model.vo.report;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单任务综合报告页模型。
 */
@Data
public class ReportDetailPageVO {

    /** 分析任务ID */
    private String taskId;

    /** 项目名称 */
    private String projectName;

    /** 远程仓库地址 */
    private String remoteUrl;

    /** Git提交SHA */
    private String commit;

    /** Git分支 */
    private String branch;

    /** 运行环境 */
    private String env;

    /** 提交作者姓名 */
    private String authorName;

    /** 提交作者邮箱 */
    private String authorEmail;

    /** 提交时间 */
    private String authorTime;

    /** 提交说明 */
    private String commitMessage;

    /** 任务状态 */
    private String status;

    /** 综合风险等级 */
    private String riskLevel;

    /** 静态风险等级 */
    private String staticRiskLevel;

    /** 静态扫描文件数 */
    private int filesScanned;

    /** 静态风险数量 */
    private int findingCount;

    /** 解析错误数量 */
    private int parseErrorCount;

    /** 静态风险卡片 */
    private List<ReportFindingCardVO> findingCards = new ArrayList<>();

    /** 动态证据列表 */
    private List<ReportDynamicEvidenceVO> dynamicEvidenceList = new ArrayList<>();

    /** 已被动态证据命中的静态风险数 */
    private int runtimeCorroboratedFindingCount;

    /** 高放大动态命中的静态风险数 */
    private int runtimeHighAmplificationCount;

    /** 综合判断文案 */
    private String conclusion;
}
