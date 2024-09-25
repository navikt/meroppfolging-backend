DROP TABLE IF EXISTS SYKMELDINGSPERIODE;
DROP TABLE IF EXISTS SYKMELDING;

CREATE TABLE SYKMELDING
(
    sykmelding_id                  VARCHAR PRIMARY KEY,
    employee_identification_number VARCHAR(11) NOT NULL,
    fom                            TIMESTAMP   NOT NULL,
    tom                            TIMESTAMP   NOT NULL,
    created_at                     TIMESTAMP   NOT NULL
);

CREATE INDEX employee_nr_index ON SYKMELDING (employee_identification_number);
