#add table for server history
CREATE TABLE serverStatusHistory (
    historyID       BIGINT              NOT NULL,
    streamID        VARCHAR(45)         NOT NULL,
    address         VARCHAR(64)         NOT NULL,
    ipAddress       CHAR(45)            NOT NULL,
    online          INT                 NOT NULL,
    type            INT                 NOT NULL,
    eventDate       CHAR(15)            NOT NULL,
    PRIMARY KEY (historyID)
);

# update version
UPDATE ofVersion SET version = 2 WHERE name = 'user-status';
