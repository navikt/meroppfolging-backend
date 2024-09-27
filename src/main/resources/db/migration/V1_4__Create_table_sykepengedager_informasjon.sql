CREATE TABLE SYKEPENGEDAGER_INFORMASJON
(

    utbetaling_id            VARCHAR PRIMARY KEY,
    person_ident             VARCHAR(11) NOT NULL,
    forelopig_beregnet_slutt TIMESTAMP   NOT NULL,
    utbetalt_tom             TIMESTAMP   NOT NULL,
    gjenstaende_sykedager    TEXT        NOT NULL,
    utbetaling_created_at    TIMESTAMP   NOT NULL,
    received_at              TIMESTAMP   NOT NULL
);

CREATE INDEX person_ident_index ON SYKEPENGEDAGER_INFORMASJON (person_ident);
