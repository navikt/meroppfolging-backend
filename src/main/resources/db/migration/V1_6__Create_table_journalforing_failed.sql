CREATE TABLE JOURNALFORING_FAILED
(

    UUID         VARCHAR PRIMARY KEY,
    varsel_uuid  VARCHAR     NOT NULL,
    pdf          bytea       NOT NULL,
    person_ident VARCHAR(11) NOT NULL,
    received_at  TIMESTAMP   NOT NULL,
    fail_reason  TEXT        NOT NULL
);

CREATE INDEX person_ident_journalforing_failed_index ON JOURNALFORING_FAILED (person_ident);
