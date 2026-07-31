package com.cmb.codeperf.demo.app.course;

import org.springframework.stereotype.Service;

/**
 * 本地课程资料客户端。
 * 用固定返回值模拟课程系统查询，保持本地业务流程可重复执行。
 */
@Service
public class LocalLearningCourseClient implements LearningCourseClient {

    /**
     * 查询课程业务编码。
     *
     * @param courseId 课程 ID
     * @return 课程业务编码
     */
    @Override
    public String getCourseCode(String courseId) {
        return "course-" + courseId;
    }
}
