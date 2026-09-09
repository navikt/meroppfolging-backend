# meroppfolging-backend

- `./gradlew build` runs build, tests and lint; `./gradlew test` runs tests.
- Local startup: `mise docker-up`, then `mise start`. The latter selects both
  `local` and `docker` Spring profiles.
- Citizen endpoints take person identity from TokenX and validate allowed
  client IDs. Veileder endpoints must also check person access through
  `VeilederTilgangClient`; Azure AD authentication alone is insufficient.
- For kartlegging lookups by response UUID or candidate ID, access must be
  checked against the person belonging to that response/candidate.
- Kartleggingsspørsmål and sen-oppfølging answers contain health information.
  Keep answers and person identifiers out of ordinary logs and metric labels.
