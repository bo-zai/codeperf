package com.cmb.codeperf.demo.app.course;

/**
 * 课程资料客户端。
 * 真实项目中该接口可能通过 RPC 或内部 SDK 查询课程系统。
 */
public interface LearningCourseClient {

    /**
     * 查询课程业务编码。
     *
     * @param courseId 课程 ID
     * @return 课程业务编码
     */
    String getCourseCode(String courseId);
}
