CREATE TABLE ESYFOVARSEL_UTBETALINGER_COPY
(
    ID INT NOT NULL PRIMARY KEY,
    FNR VARCHAR(11) NOT NULL,
    FORELOPIG_BEREGNET_SLUTT DATE,
    UTBETALT_TOM DATE,
    GJENSTAENDE_SYKEDAGER INT,
    OPPRETTET TIMESTAMP
);