CREATE TABLE SENDING_DUE_DATE
(
    sykmelding_id        VARCHAR PRIMARY KEY,
    utbetaling_id        VARCHAR     NOT NULL,
    person_ident         VARCHAR(11) NOT NULL,
    last_day_for_sending TIMESTAMP   NOT NULL,
    UNIQUE (sykmelding_id, utbetaling_id, person_ident)
);
