-- Spørring for å finne ut hvor mange ganger brukere får SSPS-varsel.
--
-- Spørringen henter ut et antall unike person-identer fra de som fikk varsel for mer enn 106 dager siden, og bruker
-- disse identene som utgangspunkt for å telle antall varsler for hver person, og så gruppere resultatet på antall
-- varsler.
-- Spørringen tar utgangspunkt i personer som fikk varsel for mer enn 106 dager siden for unngå at "falske 1-ere" blir
-- med i tellingen. Altså for å unngå å telle med tilfeller av et varsel der det er stor mulighet for at personen får et
-- nytt varsel senere.

WITH unique_person_idents AS (SELECT person_ident
                              FROM (SELECT DISTINCT person_ident, utsendt_tidspunkt
                                    FROM utsendt_varsel
                                    WHERE utsendt_tidspunkt < CURRENT_DATE - INTERVAL '106 days'
                                    ORDER BY utsendt_tidspunkt DESC
                                        LIMIT 10000) subquery), -- antall personer som det spørres mot
     person_counts AS (SELECT person_ident,
                              COUNT(*) AS varsel_count
                       FROM utsendt_varsel
                       WHERE person_ident IN (SELECT person_ident FROM unique_person_idents)
                       GROUP BY person_ident)
SELECT varsel_count AS utsendt_varsel_count,
       COUNT(*)     AS person_ident_count
FROM person_counts
GROUP BY varsel_count
ORDER BY utsendt_varsel_count;
