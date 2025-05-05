package models

import java.time.LocalDateTime
import java.util.*


data class Auftrag(
    val id: String = UUID.randomUUID().toString(),

    val sapANummer: String?,
    val startDatum: LocalDateTime?,
    val endDatum: LocalDateTime?,

    val ort: String?,
    val strecke: String?,
    val kmVon: String?,
    val kmBis: String?,

    val massnahme: String?,
    val bemerkung: String?,
    val schichten: List<Schicht>?
)

data class Schicht(
    val id: String = UUID.randomUUID().toString(),

    val startDatum: LocalDateTime?,
    val endDatum: LocalDateTime?,

    val ort: String?,
    val strecke: String?,
    val kmVon: String?,
    val kmBis: String?,
    val massnahme: String?,

    val mitarbeiter: List<Person>?,
    val fahrzeug: List<Fahrzeug>?,
    val material: List<Material>?,

    val bemerkung: String?
)

data class Person (
    val id: String = UUID.randomUUID().toString(),
    val vorname: String?,
    val name: String?,
    val firma: String?,
    val bemerkung: String?
)

data class Fahrzeug (
    val id: String = UUID.randomUUID().toString(),
    val bezeichnung: String?,
    val kennzeichen: String?,
    val bemerkung: String?
)

data class Material(
    val id: String = UUID.randomUUID().toString(),
    val bezeichnung: String?,
    val bemerkung: String?
)