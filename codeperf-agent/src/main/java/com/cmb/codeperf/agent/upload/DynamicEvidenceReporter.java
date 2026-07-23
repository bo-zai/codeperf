package com.cmb.codeperf.agent.upload;

import com.cmb.codeperf.agent.session.SessionData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/**
 * 动态证据报告器。
 * 将采集会话序列化为 JSON 并交给上传器，避免 Recorder 直接依赖 HTTP 细节。
 */
public class DynamicEvidenceReporter {

    private final DynamicEvidenceUploader uploader;
    private final ObjectMapper objectMapper;

    public DynamicEvidenceReporter(DynamicEvidenceUploader uploader) {
        this.uploader = uploader;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 上报一次会话证据。
     *
     * @param session 动态采集会话
     * @throws IOException 序列化或上传失败时抛出
     */
    public void report(SessionData session) throws IOException {
        uploader.upload(objectMapper.writeValueAsString(session));
    }
}

