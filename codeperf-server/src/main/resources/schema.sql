CREATE DATABASE IF NOT EXISTS codeperf
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE codeperf;

CREATE TABLE IF NOT EXISTS code_repository (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repo_key VARCHAR(512) NOT NULL,
  project_name VARCHAR(128) NOT NULL,
  remote_url VARCHAR(512) NOT NULL,
  provider VARCHAR(64),
  namespace VARCHAR(256),
  repo_name VARCHAR(256),
  default_branch VARCHAR(256),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_code_repository_key (repo_key),
  INDEX idx_code_repository_project (project_name),
  INDEX idx_code_repository_remote (remote_url)
);

CREATE TABLE IF NOT EXISTS git_commit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repository_id BIGINT NOT NULL,
  commit_sha VARCHAR(128) NOT NULL,
  branch_name VARCHAR(256),
  author_name VARCHAR(128),
  author_email VARCHAR(256),
  author_time VARCHAR(64),
  committer_name VARCHAR(128),
  committer_email VARCHAR(256),
  commit_message VARCHAR(1024),
  remote_url_snapshot VARCHAR(512),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_git_commit_repo_sha_branch (repository_id, commit_sha, branch_name),
  INDEX idx_git_commit_author_email (author_email),
  INDEX idx_git_commit_sha (commit_sha)
);

CREATE TABLE IF NOT EXISTS analysis_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  repository_id BIGINT NOT NULL,
  git_commit_id BIGINT NOT NULL,
  env_name VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  static_risk_level VARCHAR(32) NOT NULL DEFAULT 'NONE',
  static_payload JSON,
  dynamic_payload JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analysis_task_id (task_id),
  INDEX idx_analysis_task_repo_commit_env (repository_id, git_commit_id, env_name),
  INDEX idx_analysis_task_status (status)
);

CREATE TABLE IF NOT EXISTS static_finding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  rule_id VARCHAR(128) NOT NULL,
  severity VARCHAR(32) NOT NULL,
  confidence VARCHAR(32),
  source_file VARCHAR(1024),
  line_number INT,
  loop_start_line INT,
  loop_end_line INT,
  loop_method_name VARCHAR(256),
  io_type VARCHAR(64),
  risk_scope VARCHAR(32),
  changed_line BOOLEAN,
  introduced_by_name VARCHAR(128),
  introduced_by_email VARCHAR(256),
  introduced_commit VARCHAR(128),
  introduced_commit_time VARCHAR(64),
  evidence_hash VARCHAR(128),
  raw_payload JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_static_finding_task (task_id),
  INDEX idx_static_finding_author (introduced_by_email),
  INDEX idx_static_finding_scope (risk_scope),
  INDEX idx_static_finding_file (source_file(255))
);

CREATE TABLE IF NOT EXISTS dynamic_evidence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64),
  repository_id BIGINT,
  git_commit_id BIGINT,
  env_name VARCHAR(64),
  app_name VARCHAR(128),
  entry_key VARCHAR(256),
  raw_payload JSON NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dynamic_evidence_task (task_id),
  INDEX idx_dynamic_evidence_repo_commit_env (repository_id, git_commit_id, env_name)
);
