package com.cmb.codeperf.server.model.vo.report;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 报告列表页模型。
 */
@Data
public class ReportListPageVO {

    /** 最近分析任务 */
    private List<ReportListItemVO> tasks = new ArrayList<>();

    /** 页面展示的任务数量 */
    private int taskCount;
}
