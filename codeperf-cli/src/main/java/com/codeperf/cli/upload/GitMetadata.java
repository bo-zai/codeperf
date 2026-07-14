package com.codeperf.cli.upload;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GitMetadata {
    private final String commit;
    private final String branch;
}
