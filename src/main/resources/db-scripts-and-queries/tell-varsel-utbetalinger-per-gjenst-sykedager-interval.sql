WITH distinct_relevant_persons AS
         (SELECT DISTINCT person_ident
          FROM (SELECT person_ident
                FROM sykepengedager_informasjon
                WHERE gjenstaende_sykedager < 91
                  AND utbetaling_created_at >= NOW() - INTERVAL '4 months'
                ORDER BY utbetaling_created_at DESC) relevant_person_idents_from_last_year
          LIMIT 50000),
     first_utbetaling_row_where_gjenst_sykedager_is_low_enough AS
         (SELECT *
          FROM (SELECT *,
                       ROW_NUMBER() OVER (
                           PARTITION BY person_ident
                           ORDER BY gjenstaende_sykedager DESC
                           ) AS row_number
                FROM sykepengedager_informasjon
                WHERE gjenstaende_sykedager < 91
                  AND person_ident IN (SELECT person_ident FROM distinct_relevant_persons)) gjenstaende_sykedager_for_persons
          WHERE row_number = 1)
SELECT *
FROM (SELECT CASE
                 WHEN gjenstaende_sykedager = 90 THEN '90'
                 ELSE CONCAT(
                         TRUNC(gjenstaende_sykedager / 5) * 5,
                         '-',
                         TRUNC(gjenstaende_sykedager / 5) * 5 + 4
                      )
                 END  AS gjenstaende_sykedager_interval,
             COUNT(*) AS antall_personer
      FROM first_utbetaling_row_where_gjenst_sykedager_is_low_enough
      GROUP BY gjenstaende_sykedager_interval) subquery
ORDER BY CASE
             WHEN gjenstaende_sykedager_interval = '90' THEN 999
             ELSE CAST(SPLIT_PART(gjenstaende_sykedager_interval, '-', 1) AS INTEGER)
             END;
