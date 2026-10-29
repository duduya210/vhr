-- Monetary amounts are separate from legacy ecPoint scores.
CREATE TABLE ec_rule (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    type INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    version INT NOT NULL DEFAULT 0,
    creatorId INT NOT NULL,
    requestId VARCHAR(36) NOT NULL,
    createdAt DATETIME(3) NOT NULL,
    updatedAt DATETIME(3) NOT NULL,
    CONSTRAINT uk_ec_rule_request UNIQUE (requestId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE employeeec ADD COLUMN ruleId INT NULL;
ALTER TABLE employeeec ADD COLUMN ruleVersion INT NULL;
ALTER TABLE employeeec ADD COLUMN ruleName VARCHAR(64) NULL;
ALTER TABLE employeeec ADD COLUMN ruleDescription VARCHAR(255) NULL;
ALTER TABLE employeeec ADD COLUMN amount DECIMAL(10,2) NULL;
ALTER TABLE employeeec ADD COLUMN defaultAmount DECIMAL(10,2) NULL;
ALTER TABLE employeeec ADD COLUMN employeeName VARCHAR(64) NULL;
ALTER TABLE employeeec ADD COLUMN workId VARCHAR(32) NULL;
ALTER TABLE employeeec ADD COLUMN departmentName VARCHAR(64) NULL;
ALTER TABLE employeeec ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE employeeec ADD COLUMN operatorId INT NULL;
ALTER TABLE employeeec ADD COLUMN operatorName VARCHAR(64) NULL;
ALTER TABLE employeeec ADD COLUMN createdAt DATETIME(3) NULL;
ALTER TABLE employeeec ADD COLUMN cancelledBy INT NULL;
ALTER TABLE employeeec ADD COLUMN cancelledByName VARCHAR(64) NULL;
ALTER TABLE employeeec ADD COLUMN cancelledAt DATETIME(3) NULL;
ALTER TABLE employeeec ADD COLUMN cancelReason VARCHAR(255) NULL;
CREATE INDEX idx_employeeec_date ON employeeec(ecDate, id);
CREATE INDEX idx_employeeec_status ON employeeec(status);
-- Store immutable details and a receipt for each rewards/rules mutation.
ALTER TABLE oplog ADD COLUMN details VARCHAR(2000) NULL;
ALTER TABLE oplog ADD COLUMN requestKey VARCHAR(36) NULL;
ALTER TABLE oplog ADD COLUMN requestHash VARCHAR(64) NULL;
ALTER TABLE oplog ADD COLUMN ecAction VARCHAR(32) NULL;
ALTER TABLE oplog ADD COLUMN ecResultId INT NULL;
CREATE UNIQUE INDEX uk_oplog_request ON oplog(requestKey);

