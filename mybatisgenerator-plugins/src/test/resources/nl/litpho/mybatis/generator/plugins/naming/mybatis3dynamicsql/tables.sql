CREATE TABLE TEST
(
    ID   NUMBER(38, 0) PRIMARY KEY,
    CODE VARCHAR(6) NOT NULL,
    CONTENT BLOB
);

CREATE TABLE TEST2
(
    ID   UUID PRIMARY KEY,
    CONTENT VARCHAR(40)
);

CREATE TABLE UNRENAMED
(
    ID NUMBER(19,0) PRIMARY KEY
);
