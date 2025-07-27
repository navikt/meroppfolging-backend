WITH distinct_relevant_persons AS
         (SELECT DISTINCT person_ident
          FROM (SELECT person_ident
                FROM sykepengedager_informasjon
                WHERE gjenstaende_sykedager < 91
                  AND utbetaling_created_at >= NOW() - INTERVAL '4 months'
                ORDER BY utbetaling_created_at DESC) relevant_person_idents
          LIMIT 10000),
     utbetalinger_for_selected_persons AS
         (SELECT *
          FROM sykepengedager_informasjon
          WHERE person_ident IN (SELECT person_ident FROM distinct_relevant_persons)),
--   To avoid counting the same utbetaling twice. If forelopig_beregnet_slutt and gjenstaende_sykedager is the same,
--   it indicates the same utbetaling is registered twice.
     distinct_utbetalinger AS
         (SELECT DISTINCT ON
             (person_ident, forelopig_beregnet_slutt, gjenstaende_sykedager) person_ident,
                                                                             forelopig_beregnet_slutt,
                                                                             gjenstaende_sykedager,
                                                                             utbetaling_created_at
          FROM utbetalinger_for_selected_persons s
          ORDER BY person_ident, forelopig_beregnet_slutt, gjenstaende_sykedager, utbetaling_created_at DESC),
     ordered_utbetalinger_where_there_is_no_delay AS
         (SELECT s.*,
                 ROW_NUMBER() OVER (
                     PARTITION BY s.person_ident, s.forelopig_beregnet_slutt
                     ORDER BY s.utbetaling_created_at
                     ) AS rn
          FROM distinct_utbetalinger s),
     consecutive_utbetaling_pairs_without_delay AS
         (SELECT curr.person_ident,
                 curr.forelopig_beregnet_slutt,
                 curr.utbetaling_created_at                                   AS curr_utbetaling_created_at,
                 prev.utbetaling_created_at                                   AS prev_utbetaling_created_at,
                 EXTRACT(DAY FROM curr.utbetaling_created_at -
                                  prev.utbetaling_created_at)                 AS days_between,
                 ABS(curr.gjenstaende_sykedager - prev.gjenstaende_sykedager) AS gjenst_syked_difference

          FROM ordered_utbetalinger_where_there_is_no_delay curr
                   JOIN ordered_utbetalinger_where_there_is_no_delay prev
                        ON curr.person_ident = prev.person_ident
                            AND curr.forelopig_beregnet_slutt = prev.forelopig_beregnet_slutt
                            AND curr.rn = prev.rn + 1),
     filtered_consecutive_utbetaling_pairs_without_delay AS
         (SELECT *
          FROM consecutive_utbetaling_pairs_without_delay
          --     We want utbetalinger that has happened on different days and are registered on different days. Some
--     utbetalinger are "registered" on the same day even tough they happened on different days.
          WHERE days_between > 0
--     Disregard when there are many days between utbetalinger but the difference in gjenstaende_sykedager doesnt match
--     (because it seems sometimes utbeatlinger are registered later in bulk even though they happened over time)
            AND NOT (days_between > 30 AND gjenst_syked_difference < (days_between / 7.0 * 5 / 2)))
SELECT *
FROM (SELECT CASE
                 WHEN days_between >= 300 THEN '300+'
                 ELSE CONCAT(
                         TRUNC(days_between / 30) * 30,
                         '-',
                         TRUNC(days_between / 30) * 30 + 29
                      )
                 END  AS dager_mellom_to_etterfolgende_utbetalinger_uten_opphold,
             COUNT(*) AS antall_utbetalings_par
      FROM filtered_consecutive_utbetaling_pairs_without_delay
      GROUP BY dager_mellom_to_etterfolgende_utbetalinger_uten_opphold) subquery
ORDER BY CASE
             WHEN dager_mellom_to_etterfolgende_utbetalinger_uten_opphold = '300+' THEN 300
             ELSE CAST(SPLIT_PART(dager_mellom_to_etterfolgende_utbetalinger_uten_opphold, '-', 1) AS INTEGER)
             END;
