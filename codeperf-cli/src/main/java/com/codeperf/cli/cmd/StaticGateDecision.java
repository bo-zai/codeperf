package com.codeperf.cli.cmd;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class StaticGateDecision {
    private final boolean failed;
    private final int blocking;
    private final int newlyIntroduced;
    private final int modified;
    private final int historical;
    private final int unknown;

    public String summary() {
        return "[codeperf] gate=" + (failed ? "FAIL" : "PASS")
                + ", blocking=" + blocking
                + ", new=" + newlyIntroduced
                + ", modified=" + modified
                + ", historical=" + historical
                + ", unknown=" + unknown;
    }
}
