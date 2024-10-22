CREATE TABLE COPY_UTSENDT_VARSEL_ESYFOVARSEL
(
    uuid_esyfovarsel  VARCHAR PRIMARY KEY,
    fnr               VARCHAR(11) NOT NULL,
    type              VARCHAR     NOT NULL,
    utsendt_tidspunkt TIMESTAMP   NOT NULL
);
