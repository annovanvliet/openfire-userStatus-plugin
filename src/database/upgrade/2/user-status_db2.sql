#add table for server history
CREATE TABLE serverStatusHistory (
    historyID       BIGINT              NOT NULL,
    servername      VARCHAR(64)         NOT NULL,
    online          INT                 NOT NULL,
    ipAddress       CHAR(45)            NOT NULL,
    eventDate       CHAR(15)            NOT NULL,
    type            INT                 NOT NULL,
    PRIMARY KEY (historyID)
);

# update version
UPDATE ofVersion SET version = 2 WHERE name = 'user-status';
