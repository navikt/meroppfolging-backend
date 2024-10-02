CREATE TABLE UTSENDT_VARSEL
(
    uuid              UUID PRIMARY KEY,
    person_ident      VARCHAR(11) NOT NULL,
    utsendt_tidspunkt TIMESTAMP   NOT NULL,
    utbetaling_id     VARCHAR     NOT NULL,
    sykmelding_id     VARCHAR     NOT NULL
);

CREATE INDEX utsendt_varsel_fnr_index ON UTSENDT_VARSEL (person_ident);
