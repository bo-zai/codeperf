package com.cmb.codeperf.demo.app.service;

import com.cmb.codeperf.demo.app.course.LearningCourseClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户业务服务。
 */
@Service
public class UserService {

    private final LearningCourseClient learningCourseClient;

    public UserService(LearningCourseClient learningCourseClient) {
        this.learningCourseClient = learningCourseClient;
    }

    /**
     * 根据课程 ID 查询业务侧编码。
     *
     * @param courseIds 课程 ID 列表
     * @return 业务侧补全后的课程编码
     */
    public List<String> getCourseCodes(List<String> courseIds) {
        List<String> values = new ArrayList<String>();
        for (String courseId : courseIds) {
            values.add(learningCourseClient.getCourseCode(courseId));
        }
        return values;
    }
}

