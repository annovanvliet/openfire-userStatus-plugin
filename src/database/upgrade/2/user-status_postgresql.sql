#add table for server history
CREATE TABLE serverStatusHistory (
    historyID       BIGINT              NOT NULL,
    streamID        VARCHAR(45)         NOT NULL,
    address         VARCHAR(64)         NOT NULL,
    ipAddress       CHAR(45)            NOT NULL,
    online          INTEGER             NOT NULL,
    type            INTEGER             NOT NULL,
    eventDate       CHAR(15)            NOT NULL,
    constraint pk_serverStatusHistory PRIMARY KEY (historyID)
);

# update version
UPDATE ofVersion SET version = 2 WHERE name = 'user-status';
