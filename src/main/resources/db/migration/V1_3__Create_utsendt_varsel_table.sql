CREATE TABLE UTSENDT_VARSEL
(
    uuid              UUID PRIMARY KEY,
    fnr               VARCHAR(11)  NOT NULL,
    utsendt_tidspunkt TIMESTAMP    NOT NULL,
    sykepengedager_id VARCHAR(100) NOT NULL
);

CREATE INDEX utsendt_varsel_fnr_index ON UTSENDT_VARSEL (fnr);
