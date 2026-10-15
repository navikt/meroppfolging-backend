package no.nav.syfo.kartlegging.exception

class KartleggingAlreadyRespondedException(message: String = "Kandidaten har allerede svart på kartleggingsspørsmål") :
    RuntimeException(message)
