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
                val schichten:     List<Schicht>       = emptyList(),
                val stundenzettel: List<Stundenzettel> = emptyList()
            )

            // models/Schicht.kt
            data class Schicht(
                val id:          String           = UUID.randomUUID().toString(),
                val startDatum:  LocalDateTime?   = null,
                val endDatum:    LocalDateTime?   = null,
                val ort:         String?          = null,
                val strecke:     String?          = null,
                val kmVon:       String?          = null,
                val kmBis:       String?          = null,
                val massnahme:   String?          = null,
                val bemerkung:   String?          = null,

                /* ───── neue Zeile: ───── */
                val pausenZeit:  Int             = 0,          // Minuten

                /* Relationen  … */
                val mitarbeiter: List<Person>    = emptyList(),
                val fahrzeug:    List<Fahrzeug>  = emptyList(),
                val material:    List<Material>  = emptyList()
            )


            data class Person(
                val id: String = UUID.randomUUID().toString(),
                val vorname: String?,
                val name: String?,
                val firma: String?,
                val bemerkung: String?,
                val position: Int = 0            // NEU
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

            data class Stundenzettel(
                val id: String = UUID.randomUUID().toString(),
                val startDatum: LocalDateTime?,
                val endDatum: LocalDateTime?,
                val pfad: String?

            )