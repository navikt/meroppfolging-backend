package no.nav.syfo.sykmelding.service

import no.nav.syfo.sykmelding.database.SykmeldingDao
import no.nav.syfo.sykmelding.domain.PSykmelding
import no.nav.syfo.sykmelding.domain.Periode
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SykmeldingService(private val sykmeldingDao: SykmeldingDao) {
    fun persistSykmelding(
        sykmeldingId: String,
        employeeIdentificationNumber: String,
        sykmeldingsperioder: List<Periode>,
    ) {
        if (sykmeldingsperioder.maxOf { it.tom } < LocalDate.now()) {
            return
        }

        sykmeldingDao.persistSykmelding(
            sykmeldingId = sykmeldingId,
            employeeIdentificationNumber = employeeIdentificationNumber,
            fom = sykmeldingsperioder.minOf { it.fom },
            tom = sykmeldingsperioder.maxOf { it.tom },
        )
    }

    fun deleteSykmelding(sykmeldingId: String) {
        sykmeldingDao.deleteSykmelding(sykmeldingId)
    }

    fun getSykmelding(employeeIdentificationNumber: String,): PSykmelding? =
        sykmeldingDao.getSykmelding(employeeIdentificationNumber)
}
