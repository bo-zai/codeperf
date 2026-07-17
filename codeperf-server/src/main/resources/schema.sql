CREATE DATABASE IF NOT EXISTS codeperf
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE codeperf;

CREATE TABLE IF NOT EXISTS code_repository (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  repo_key VARCHAR(512) NOT NULL COMMENT '仓库唯一标识，优先使用归一化后的远程仓库地址',
  project_name VARCHAR(128) NOT NULL COMMENT '项目名称',
  remote_url VARCHAR(512) NOT NULL COMMENT 'Git远程仓库地址',
  provider VARCHAR(64) COMMENT '代码托管平台，例如github、gitlab、gitee',
  namespace VARCHAR(256) COMMENT '仓库命名空间或组织',
  repo_name VARCHAR(256) COMMENT '仓库名称',
  default_branch VARCHAR(256) COMMENT '默认分支',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_code_repository_key (repo_key),
  INDEX idx_code_repository_project (project_name),
  INDEX idx_code_repository_remote (remote_url)
) COMMENT='代码仓库表';

CREATE TABLE IF NOT EXISTS codeperf_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id VARCHAR(128) NOT NULL COMMENT '企业用户唯一标识',
  user_name VARCHAR(128) COMMENT '用户姓名',
  sap_id VARCHAR(128) COMMENT 'SAP人员编号',
  email VARCHAR(256) NOT NULL COMMENT '用户邮箱',
  im_account VARCHAR(256) COMMENT '即时通讯账号',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_codeperf_user_user_id (user_id),
  UNIQUE KEY uk_codeperf_user_email (email),
  INDEX idx_codeperf_user_user_name (user_name),
  INDEX idx_codeperf_user_sap_id (sap_id),
  INDEX idx_codeperf_user_im_account (im_account)
) COMMENT='CodePerf用户表';

CREATE TABLE IF NOT EXISTS git_commit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  repository_id BIGINT NOT NULL COMMENT '代码仓库ID，逻辑关联code_repository.id',
  commit_sha VARCHAR(128) NOT NULL COMMENT 'Git提交SHA',
  branch_name VARCHAR(256) COMMENT '分支名称',
  author_name VARCHAR(128) COMMENT '提交作者姓名',
  author_email VARCHAR(256) COMMENT '提交作者邮箱',
  author_time DATETIME COMMENT '提交作者时间',
  committer_name VARCHAR(128) COMMENT '提交执行人姓名',
  committer_email VARCHAR(256) COMMENT '提交执行人邮箱',
  commit_message VARCHAR(1024) COMMENT '提交说明',
  remote_url_snapshot VARCHAR(512) COMMENT '提交时采集到的远程仓库地址快照',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_git_commit_repo_sha_branch (repository_id, commit_sha, branch_name),
  INDEX idx_git_commit_author_email (author_email),
  INDEX idx_git_commit_sha (commit_sha)
) COMMENT='Git提交信息表';

CREATE TABLE IF NOT EXISTS analysis_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id VARCHAR(64) NOT NULL COMMENT '分析任务唯一ID',
  repository_id BIGINT NOT NULL COMMENT '代码仓库ID，逻辑关联code_repository.id',
  git_commit_id BIGINT NOT NULL COMMENT 'Git提交ID，逻辑关联git_commit.id',
  env_name VARCHAR(64) COMMENT '环境名称，例如local、dev、preprod',
  status VARCHAR(32) NOT NULL COMMENT '任务状态',
  risk_level VARCHAR(32) NOT NULL COMMENT '综合风险等级',
  static_risk_level VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '静态扫描风险等级',
  static_payload JSON COMMENT '静态扫描原始报告JSON',
  dynamic_payload JSON COMMENT '动态证据原始报告JSON',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_analysis_task_id (task_id),
  INDEX idx_analysis_task_repo_commit_env (repository_id, git_commit_id, env_name),
  INDEX idx_analysis_task_status (status)
) COMMENT='分析任务表';

CREATE TABLE IF NOT EXISTS agent_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  session_id VARCHAR(64) NOT NULL COMMENT 'Agent运行会话唯一ID',
  repository_id BIGINT COMMENT '代码仓库ID，逻辑关联code_repository.id',
  git_commit_id BIGINT COMMENT 'Git提交ID，逻辑关联git_commit.id',
  task_id VARCHAR(64) COMMENT '分析任务ID，逻辑关联analysis_task.task_id',
  env_name VARCHAR(64) NOT NULL COMMENT '运行环境名称',
  app_name VARCHAR(128) NOT NULL COMMENT '应用名称',
  host_name VARCHAR(256) COMMENT '主机名',
  instance_id VARCHAR(256) COMMENT '应用实例ID',
  agent_version VARCHAR(64) COMMENT 'Agent版本',
  started_at VARCHAR(64) COMMENT '会话开始时间',
  ended_at VARCHAR(64) COMMENT '会话结束时间',
  status VARCHAR(32) NOT NULL DEFAULT 'RUNNING' COMMENT '会话状态',
  raw_payload JSON COMMENT 'Agent会话原始上下文JSON',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_agent_session_id (session_id),
  INDEX idx_agent_session_task (task_id),
  INDEX idx_agent_session_repo_commit_env (repository_id, git_commit_id, env_name),
  INDEX idx_agent_session_app_env (app_name, env_name)
) COMMENT='Agent运行会话表';

CREATE TABLE IF NOT EXISTS rule_definition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  rule_id VARCHAR(128) NOT NULL COMMENT '规则唯一标识',
  rule_name VARCHAR(256) NOT NULL COMMENT '规则名称',
  category VARCHAR(64) NOT NULL COMMENT '规则分类',
  detector_type VARCHAR(32) NOT NULL COMMENT '检测类型，静态或动态',
  default_severity VARCHAR(32) NOT NULL COMMENT '默认严重级别',
  enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
  version VARCHAR(64) COMMENT '规则版本',
  description VARCHAR(1024) COMMENT '规则说明',
  remediation VARCHAR(2048) COMMENT '修复建议',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_rule_definition_rule_id (rule_id),
  INDEX idx_rule_definition_category (category),
  INDEX idx_rule_definition_enabled (enabled)
) COMMENT='规则定义表';

INSERT IGNORE INTO rule_definition (
  rule_id,
  rule_name,
  category,
  detector_type,
  default_severity,
  enabled,
  version,
  description,
  remediation
) VALUES (
  'LOOP_IO_AMPLIFICATION',
  '循环内I/O放大风险',
  'LOOP_IO',
  'STATIC',
  'WARN',
  TRUE,
  '1.0.0',
  '检测循环体内直接或间接调用数据库、缓存、HTTP、RPC等外部I/O的风险。',
  '将循环内I/O改为批量查询、批量调用、缓存预取或在循环外预加载，避免生产数据量放大请求耗时。'
);

CREATE TABLE IF NOT EXISTS static_finding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id VARCHAR(64) NOT NULL COMMENT '分析任务ID，逻辑关联analysis_task.task_id',
  rule_id VARCHAR(128) NOT NULL COMMENT '规则ID，逻辑关联rule_definition.rule_id',
  severity VARCHAR(32) NOT NULL COMMENT '严重级别',
  confidence VARCHAR(32) COMMENT '置信度',
  source_file VARCHAR(1024) COMMENT '源码文件路径',
  line_number INT COMMENT '风险代码行号',
  loop_start_line INT COMMENT '循环起始行号',
  loop_end_line INT COMMENT '循环结束行号',
  loop_method_name VARCHAR(256) COMMENT '循环所在方法名',
  io_type VARCHAR(64) COMMENT 'I/O类型，例如DB、HTTP、RPC、SDK',
  risk_scope VARCHAR(32) COMMENT '风险范围，例如NEW、HISTORICAL',
  changed_line BOOLEAN COMMENT '是否命中本次变更行',
  introduced_by_name VARCHAR(128) COMMENT '引入人姓名',
  introduced_by_email VARCHAR(256) COMMENT '引入人邮箱',
  introduced_commit VARCHAR(128) COMMENT '引入风险的提交SHA',
  introduced_commit_time VARCHAR(64) COMMENT '引入风险的提交时间',
  evidence_hash VARCHAR(128) COMMENT '证据哈希，用于问题去重',
  raw_payload JSON COMMENT '静态发现原始JSON',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_static_finding_task (task_id),
  INDEX idx_static_finding_author (introduced_by_email),
  INDEX idx_static_finding_scope (risk_scope),
  INDEX idx_static_finding_file (source_file(255))
) COMMENT='静态扫描发现表';

CREATE TABLE IF NOT EXISTS dynamic_evidence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id VARCHAR(64) NOT NULL COMMENT '分析任务ID，逻辑关联analysis_task.task_id',
  agent_session_id BIGINT COMMENT 'Agent会话ID，逻辑关联agent_session.id',
  repository_id BIGINT COMMENT '代码仓库ID，逻辑关联code_repository.id',
  git_commit_id BIGINT COMMENT 'Git提交ID，逻辑关联git_commit.id',
  env_name VARCHAR(64) COMMENT '环境名称',
  app_name VARCHAR(128) COMMENT '应用名称',
  entry_key VARCHAR(256) COMMENT '入口方法或接口标识',
  raw_payload JSON NOT NULL COMMENT '动态证据原始JSON',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_dynamic_evidence_task (task_id),
  INDEX idx_dynamic_evidence_agent_session (agent_session_id),
  INDEX idx_dynamic_evidence_entry (entry_key),
  INDEX idx_dynamic_evidence_repo_commit_env (repository_id, git_commit_id, env_name)
) COMMENT='动态运行证据表';

CREATE TABLE IF NOT EXISTS finding_issue (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  issue_key VARCHAR(128) NOT NULL COMMENT '问题唯一标识',
  repository_id BIGINT NOT NULL COMMENT '代码仓库ID，逻辑关联code_repository.id',
  rule_id VARCHAR(128) NOT NULL COMMENT '规则ID，逻辑关联rule_definition.rule_id',
  evidence_hash VARCHAR(128) NOT NULL COMMENT '证据哈希，用于问题归并',
  source_file VARCHAR(1024) COMMENT '源码文件路径',
  line_number INT COMMENT '问题代码行号',
  owner_email VARCHAR(256) COMMENT '问题负责人邮箱',
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT '问题状态',
  severity VARCHAR(32) NOT NULL COMMENT '严重级别',
  first_seen_task_id VARCHAR(64) COMMENT '首次发现任务ID',
  last_seen_task_id VARCHAR(64) COMMENT '最近发现任务ID',
  fixed_task_id VARCHAR(64) COMMENT '修复确认任务ID',
  first_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次发现时间',
  last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最近发现时间',
  fixed_at DATETIME NULL COMMENT '修复时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_finding_issue_key (issue_key),
  INDEX idx_finding_issue_repo_status (repository_id, status),
  INDEX idx_finding_issue_owner (owner_email),
  INDEX idx_finding_issue_rule (rule_id),
  INDEX idx_finding_issue_file (source_file(255))
) COMMENT='问题生命周期表';

CREATE TABLE IF NOT EXISTS finding_occurrence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  issue_id BIGINT NOT NULL COMMENT '问题ID，逻辑关联finding_issue.id',
  task_id VARCHAR(64) NOT NULL COMMENT '分析任务ID，逻辑关联analysis_task.task_id',
  finding_id BIGINT COMMENT '静态发现ID，逻辑关联static_finding.id',
  occurrence_type VARCHAR(32) NOT NULL COMMENT '出现类型，例如NEW、EXISTING、FIXED',
  risk_scope VARCHAR(32) COMMENT '风险范围',
  severity VARCHAR(32) NOT NULL COMMENT '严重级别',
  confidence VARCHAR(32) COMMENT '置信度',
  raw_payload JSON COMMENT '出现记录原始JSON',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_finding_occurrence_issue_task (issue_id, task_id),
  INDEX idx_finding_occurrence_task (task_id),
  INDEX idx_finding_occurrence_finding (finding_id)
) COMMENT='问题出现记录表';

CREATE TABLE IF NOT EXISTS scan_baseline (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  baseline_key VARCHAR(256) NOT NULL COMMENT '基线唯一标识',
  repository_id BIGINT NOT NULL COMMENT '代码仓库ID，逻辑关联code_repository.id',
  branch_name VARCHAR(256) NOT NULL COMMENT '基线分支名称',
  task_id VARCHAR(64) NOT NULL COMMENT '形成基线的分析任务ID',
  commit_sha VARCHAR(128) NOT NULL COMMENT '形成基线的提交SHA',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '基线状态',
  created_by_email VARCHAR(256) COMMENT '创建人邮箱',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_scan_baseline_key (baseline_key),
  INDEX idx_scan_baseline_repo_branch (repository_id, branch_name),
  INDEX idx_scan_baseline_task (task_id)
) COMMENT='扫描基线表';

CREATE TABLE IF NOT EXISTS risk_waiver (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  issue_id BIGINT NOT NULL COMMENT '问题ID，逻辑关联finding_issue.id',
  repository_id BIGINT NOT NULL COMMENT '代码仓库ID，逻辑关联code_repository.id',
  reason VARCHAR(2048) NOT NULL COMMENT '豁免原因',
  approved_by_email VARCHAR(256) NOT NULL COMMENT '审批人邮箱',
  expires_at VARCHAR(64) COMMENT '豁免过期时间',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '豁免状态',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_risk_waiver_issue (issue_id),
  INDEX idx_risk_waiver_repo_status (repository_id, status),
  INDEX idx_risk_waiver_approver (approved_by_email)
) COMMENT='风险豁免表';

CREATE TABLE IF NOT EXISTS notification_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id VARCHAR(64) COMMENT '分析任务ID，逻辑关联analysis_task.task_id',
  issue_id BIGINT COMMENT '问题ID，逻辑关联finding_issue.id',
  repository_id BIGINT COMMENT '代码仓库ID，逻辑关联code_repository.id',
  git_commit_id BIGINT COMMENT 'Git提交ID，逻辑关联git_commit.id',
  recipient_user_id BIGINT COMMENT '接收人用户表主键ID，逻辑关联codeperf_user.id',
  recipient_user_key VARCHAR(128) COMMENT '接收人企业用户ID快照',
  recipient_name VARCHAR(128) COMMENT '接收人姓名快照',
  recipient_email VARCHAR(256) COMMENT '接收人邮箱快照',
  match_source VARCHAR(64) COMMENT '用户匹配来源，例如AUTHOR_EMAIL、COMMITTER_EMAIL、AUTHOR_NAME、SAP_ID',
  channel VARCHAR(64) NOT NULL COMMENT '通知渠道',
  template_code VARCHAR(128) COMMENT '通知模板编码',
  status VARCHAR(32) NOT NULL COMMENT '发送状态',
  error_message VARCHAR(1024) COMMENT '发送失败原因',
  sent_at VARCHAR(64) COMMENT '发送时间',
  raw_payload JSON COMMENT '通知上下文原始JSON',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_notification_record_task (task_id),
  INDEX idx_notification_record_issue (issue_id),
  INDEX idx_notification_record_user (recipient_user_id),
  INDEX idx_notification_record_recipient (recipient_email),
  INDEX idx_notification_record_status (status)
) COMMENT='通知发送记录表';
