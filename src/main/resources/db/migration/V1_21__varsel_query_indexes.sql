CREATE INDEX idx_sykmelding_ident_created ON SYKMELDING (employee_identification_number, created_at DESC);
CREATE INDEX idx_spdi_ident_utbetaling_created ON SYKEPENGEDAGER_INFORMASJON (person_ident, utbetaling_created_at DESC);
CREATE INDEX idx_utsendt_varsel_ident_tidspunkt ON UTSENDT_VARSEL (person_ident, utsendt_tidspunkt);
CREATE INDEX idx_copy_utsendt_varsel_fnr_tidspunkt ON COPY_UTSENDT_VARSEL_ESYFOVARSEL (fnr, utsendt_tidspunkt);
