#add table for server history
CREATE TABLE serverStatusHistory (
    historyID       BIGINT              NOT NULL,
    servername      VARCHAR(64)         NOT NULL,
    online          TINYINT             NOT NULL,
    ipAddress       CHAR(45)            NOT NULL,
    eventDate       CHAR(15)            NOT NULL,
    type            TINYINT             NOT NULL,
    PRIMARY KEY pk_serverStatusHistory (historyID)
);

# update version
UPDATE ofVersion SET version = 2 WHERE name = 'user-status';
