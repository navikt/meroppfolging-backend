package no.nav.syfo.utils


const val PROD_GCP = "prod-gcp"


fun isProd(): Boolean = PROD_GCP == System.getenv("NAIS_CLUSTER_NAME")
