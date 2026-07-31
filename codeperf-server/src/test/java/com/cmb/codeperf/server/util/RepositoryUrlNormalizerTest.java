package com.cmb.codeperf.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Git 仓库地址规范化测试。
 * 静态扫描与动态 agent 的运行环境不同，必须确保不同协议的同一仓库能生成相同 repoKey。
 */
public class RepositoryUrlNormalizerTest {

    @Test
    public void should_ReturnSameRepoKey_When_SshAndHttpsPointToSameRepository() {
        String ssh = "git@gitee.itc.cmbchina.cn:S992391/LQ13.10_demand-impact-analysis.git";
        String https = "https://gitee.itc.cmbchina.cn/S992391/LQ13.10_demand-impact-analysis.git";

        assertEquals(RepositoryUrlNormalizer.toRepoKey(ssh), RepositoryUrlNormalizer.toRepoKey(https));
        assertEquals("gitee.itc.cmbchina.cn/s992391/lq13.10_demand-impact-analysis",
                RepositoryUrlNormalizer.toRepoKey(ssh));
    }

    @Test
    public void should_NormalizeGitUrl_When_UrlContainsUserProtocolAndTail() {
        String value = "ssh://git@GITEE.ITC.CMBCHINA.CN/S992391/demo.git/";

        assertEquals("gitee.itc.cmbchina.cn/s992391/demo", RepositoryUrlNormalizer.toRepoKey(value));
    }
}
