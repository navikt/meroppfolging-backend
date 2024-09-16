DROP TABLE IF EXISTS SYKMELDINGSPERIODE;

CREATE TABLE SYKMELDINGSPERIODE
(
    sykmelding_id                  VARCHAR PRIMARY KEY,
    has_employer                   BOOLEAN     NOT NULL,
    employee_identification_number VARCHAR(11) NOT NULL,
    fom                            TIMESTAMP   NOT NULL,
    tom                            TIMESTAMP   NOT NULL,
    created_at                     TIMESTAMP   NOT NULL
);

CREATE INDEX employee_nr_index ON SYKMELDINGSPERIODE (employee_identification_number);
