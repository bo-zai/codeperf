package com.codeperf.agent.collect;

import com.codeperf.agent.session.SessionData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 把 SessionData 序列化为 JSON 写到 output，并写一个 ${output}.done 标记文件，
 * 供 CLI 端轮询判定采集完成。见 docs/02-agent-core.md 第 7、8 节。
 * 路径使用跨平台 API（可移植性决策④）。
 */
public class SessionWriter {

    private final Path output;
    private final ObjectMapper mapper;

    public SessionWriter(String outputPath) {
        this.output = Paths.get(outputPath).toAbsolutePath();
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public synchronized void write(SessionData session) {
        try {
            File parent = output.getParent() == null ? null : output.getParent().toFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            byte[] json = mapper.writeValueAsBytes(session);
            Files.write(output, json);
            Path done = Paths.get(output.toString() + ".done");
            Files.write(done, Long.toString(System.currentTimeMillis()).getBytes("UTF-8"));
            System.out.println("[codeperf] session data written to " + output);
        } catch (Throwable t) {
            System.err.println("[codeperf] failed to write session data: " + t);
        }
    }

    public Path getOutput() {
        return output;
    }
}
