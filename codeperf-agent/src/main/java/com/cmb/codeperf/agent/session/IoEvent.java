package com.cmb.codeperf.agent.session;

import lombok.Getter;
import lombok.Setter;

/**
 * 请求内发生的一次外部 I/O 语义事件。
 * 该模型用于弥补框架代理、拦截器导致业务调用树中缺少真实 Mapper/HTTP/RPC 方法的问题。
 */
@Getter
@Setter
public class IoEvent {

    /** I/O 类型：DB、HTTP、RPC。 */
    private String ioType;

    /** 采集来源框架，例如 MYBATIS、SPRING_REST_TEMPLATE、DUBBO。 */
    private String framework;

    /** 操作类型，例如 SELECT、UPDATE、GET、POST、RPC 方法名。 */
    private String operation;

    /** I/O 目标，DB 为 Mapper 方法，HTTP 为 URI，RPC 为服务方法。 */
    private String target;

    /** MyBatis MappedStatement ID，用于绕过 Interceptor 直接还原 Mapper 方法。 */
    private String mappedStatementId;

    /** I/O 所属类名，例如 Mapper 接口或 RPC 服务接口。 */
    private String className;

    /** I/O 方法名，例如 selectById 或 queryStock。 */
    private String methodName;

    /** 触发 I/O 时的业务调用路径，用于报告解释从入口到 I/O 的运行路径。 */
    private String businessCallPath;

    /** 同一请求内相同 I/O 事件的重复次数。 */
    private int count;

    /** 同一请求内相同 I/O 事件的累计耗时，单位毫秒。 */
    private long elapsedMs;
}
