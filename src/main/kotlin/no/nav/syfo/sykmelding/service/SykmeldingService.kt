package no.nav.syfo.sykmelding.service

import no.nav.syfo.sykmelding.database.SykmeldingDao
import no.nav.syfo.sykmelding.domain.Periode
import no.nav.syfo.sykmelding.domain.Sykmeldingsperiode
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SykmeldingService(private val sykmeldingDao: SykmeldingDao) {
    fun persistSykmeldingsperioder(
        sykmeldingId: String,
        employeeIdentificationNumber: String,
        sykmeldingsperioder: List<Periode>,
        harArbeidsgiver: Boolean,
    ) {
        val activeSykmeldingsPerioder = sykmeldingsperioder.filter {
            !it.tom.isBefore(
                LocalDate.now(),
            )
        }
        activeSykmeldingsPerioder.forEach { sykmeldingsperiode ->
            sykmeldingDao.persistSykmeldingsperiode(
                sykmeldingId = sykmeldingId,
                employeeIdentificationNumber = employeeIdentificationNumber,
                fom = sykmeldingsperiode.fom,
                tom = sykmeldingsperiode.tom,
                harArbeidsgiver = harArbeidsgiver,
            )
        }
    }

    fun deleteSykmeldingsperioder(sykmeldingId: String) {
        sykmeldingDao.deleteSykmeldingsperioder(sykmeldingId)
    }

    fun getSykmeldingsperioder(
        employeeIdentificationNumber: String,
    ): List<Sykmeldingsperiode> {
        return sykmeldingDao.getSykmeldingsperioder(employeeIdentificationNumber)
    }
}
