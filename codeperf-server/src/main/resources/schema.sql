CREATE TABLE IF NOT EXISTS analysis_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL UNIQUE,
  project VARCHAR(128) NOT NULL,
  branch_name VARCHAR(256),
  commit_sha VARCHAR(128),
  env_name VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_analysis_task_project_commit (project, commit_sha),
  INDEX idx_analysis_task_status (status)
);

CREATE TABLE IF NOT EXISTS static_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  raw_payload JSON NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_static_result_task (task_id)
);

CREATE TABLE IF NOT EXISTS dynamic_evidence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  entry_key VARCHAR(256),
  raw_payload JSON NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dynamic_evidence_task (task_id)
);

CREATE TABLE IF NOT EXISTS production_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  source_name VARCHAR(128),
  raw_payload JSON NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_production_profile_task (task_id)
);

CREATE TABLE IF NOT EXISTS analysis_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL UNIQUE,
  risk_level VARCHAR(32) NOT NULL,
  raw_payload JSON NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_analysis_report_task (task_id)
);
