package com.codeperf.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaticFindingSummary {
    private String type;
    private String severity;
    private String confidence;
    private String sourceFile;
    private int lineNumber;
    private int loopStartLine;
    private int loopEndLine;
    private String ioType;
    private String loopMethodName;
    private int loopCallLine;
    private int ioLine;
}
