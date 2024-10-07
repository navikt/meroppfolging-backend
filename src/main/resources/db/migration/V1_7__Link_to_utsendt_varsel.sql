DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name='form_response'
                         AND column_name='utsendt_varsel_uuid') THEN
            ALTER TABLE form_response ADD COLUMN utsendt_varsel_uuid UUID;
        END IF;
    END $$;
