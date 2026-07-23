package com.cmb.codeperf.analysis.source.match;

import com.cmb.codeperf.analysis.source.SourceFinding;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IoMatch {
    private final boolean matched;
    private final String ioType;
    private final SourceFinding.Confidence confidence;
    private final String reason;

    public static IoMatch none() {
        return new IoMatch(false, null, SourceFinding.Confidence.LOW, "");
    }
}

