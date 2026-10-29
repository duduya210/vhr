-- Additive migration: preserve legacy rows without inventing historical snapshots.
ALTER TABLE employeeremove ADD COLUMN beforeDepId INT NULL;
ALTER TABLE employeeremove ADD COLUMN beforeJobId INT NULL;
ALTER TABLE employeeremove ADD COLUMN employeeName VARCHAR(64) NULL;
ALTER TABLE employeeremove ADD COLUMN workId VARCHAR(32) NULL;
ALTER TABLE employeeremove ADD COLUMN beforeDepName VARCHAR(64) NULL;
ALTER TABLE employeeremove ADD COLUMN beforeJobName VARCHAR(64) NULL;
ALTER TABLE employeeremove ADD COLUMN afterDepName VARCHAR(64) NULL;
ALTER TABLE employeeremove ADD COLUMN afterJobName VARCHAR(64) NULL;
ALTER TABLE employeeremove ADD COLUMN operatorId INT NULL;
ALTER TABLE employeeremove ADD COLUMN operatorName VARCHAR(64) NULL;
ALTER TABLE employeeremove ADD COLUMN createdAt DATETIME(3) NULL;
ALTER TABLE employeeremove ADD COLUMN requestId VARCHAR(36) NULL;
CREATE UNIQUE INDEX uk_employeeremove_request ON employeeremove(requestId);
CREATE INDEX idx_employeeremove_created ON employeeremove(createdAt, id);
CREATE INDEX idx_employeeremove_department ON employeeremove(afterDepId);

ALTER TABLE oplog ADD COLUMN transferId INT NULL;
ALTER TABLE oplog ADD COLUMN operatorName VARCHAR(64) NULL;
ALTER TABLE oplog ADD COLUMN createdAt DATETIME(3) NULL;
CREATE UNIQUE INDEX uk_oplog_transfer ON oplog(transferId);
CREATE INDEX idx_oplog_created ON oplog(createdAt, id);

