-- 搭子模块增量迁移（已部署数据库执行一次）
ALTER TABLE mate_invitation
  ADD INDEX idx_public_activity (status, review_status, activity_time),
  ADD INDEX idx_category_activity (category, status, review_status, activity_time);

ALTER TABLE mate_participant
  ADD COLUMN apply_count INT NOT NULL DEFAULT 1,
  ADD COLUMN last_applied_at DATETIME NULL,
  ADD INDEX idx_mate_participant_status (invitation_id, status, created_at),
  ADD INDEX idx_mate_user_status (user_id, status, created_at);

UPDATE mate_participant
SET last_applied_at = created_at
WHERE last_applied_at IS NULL;
