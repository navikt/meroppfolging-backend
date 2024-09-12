CREATE TABLE SYKEPENGEDAGER
(
    uuid                 UUID PRIMARY KEY,
    fnr                  VARCHAR(11)  NOT NULL,
    maksDato             TIMESTAMP    NOT NULL,
    utbetaltTom          TIMESTAMP    NOT NULL,
    gjenstaendeSykedager INT          NOT NULL,
    sykepengedager_id    VARCHAR(100) NOT NULL,
    opprettet            TIMESTAMP    NOT NULL
);

CREATE INDEX sykepengedager_fnr_index ON SYKEPENGEDAGER (fnr);
