package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.service.impl.ReportPageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Server 内置报告页面控制器。
 * 页面用于开发者从通知或浏览器直接查看综合检测结果，不替代 JSON API。
 */
@Controller
public class ReportPageController {

    private final ReportPageService reportPageService;

    public ReportPageController(ReportPageService reportPageService) {
        this.reportPageService = reportPageService;
    }

    /**
     * 展示最近分析任务。
     *
     * @param model 页面模型
     * @return Thymeleaf 模板路径
     */
    @GetMapping("/reports")
    public String list(Model model) {
        model.addAttribute("page", reportPageService.getListPage());
        return "reports/list";
    }

    /**
     * 展示单任务综合检测报告。
     *
     * @param taskId 分析任务ID
     * @param model 页面模型
     * @return Thymeleaf 模板路径
     */
    @GetMapping("/reports/{taskId}")
    public String detail(@PathVariable String taskId, Model model) {
        model.addAttribute("page", reportPageService.getDetailPage(taskId));
        return "reports/detail";
    }

    /**
     * 展示分支综合报告。
     *
     * @param remoteUrl 远程仓库地址
     * @param branch 分支名称
     * @param env 环境名称
     * @param model 页面模型
     * @return Thymeleaf 模板路径
     */
    @GetMapping("/reports/branches/latest")
    public String branchLatest(@RequestParam String remoteUrl,
                               @RequestParam String branch,
                               @RequestParam(defaultValue = "dev") String env,
                               Model model) {
        model.addAttribute("page", reportPageService.getBranchPage(remoteUrl, branch, env));
        return "reports/branch";
    }
}
