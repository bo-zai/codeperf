package com.codeperf.cli.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ModuleScanConfig {
    private String name;
    private List<String> sourceRoots = new ArrayList<>();
    private List<String> targetPackages = new ArrayList<>();
}
