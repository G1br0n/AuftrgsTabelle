package repository

import models.*
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime

class AuftragRepository(
    private val dbPath: String = "jdbc:sqlite:auftrag.db"
) {

    /* ────────────── DB‑Initialisierung mit Schema-Versionierung ────────────── */

    init { createTables() }

    private fun getConnection(): Connection = DriverManager.getConnection(dbPath)

    private fun createTables() {
        getConnection().use { conn ->
            conn.createStatement().use { st ->
                // 1) Aktuelle Datenbank-Version lesen (PRAGMA user_version)
                val rsVersion = st.executeQuery("PRAGMA user_version;")
                val version = if (rsVersion.next()) rsVersion.getInt(1) else 0

                // Migration auf Version 1: Basisschema
                if (version < 1) {
                    st.execute("""
                        CREATE TABLE IF NOT EXISTS auftrag(
                            id TEXT PRIMARY KEY,
                            sapANummer TEXT,
                            startDatum TEXT,
                            endDatum   TEXT,
                            ort TEXT, strecke TEXT, kmVon TEXT, kmBis TEXT,
                            massnahme TEXT, bemerkung TEXT
                        );
                    """.trimIndent())

                    st.execute("""
                        CREATE TABLE IF NOT EXISTS schicht(
                            id TEXT PRIMARY KEY,
                            auftrag_id TEXT,
                            startDatum TEXT,
                            endDatum   TEXT,
                            ort TEXT, strecke TEXT, kmVon TEXT, kmBis TEXT,
                            massnahme TEXT,
                            bemerkung TEXT,
                            pausenZeit INTEGER DEFAULT 0,
                            FOREIGN KEY(auftrag_id) REFERENCES auftrag(id)
                        );
                    """.trimIndent())

                    st.execute("""
                        CREATE TABLE IF NOT EXISTS person(
                            id TEXT PRIMARY KEY,
                            vorname TEXT, name TEXT, firma TEXT, bemerkung TEXT
                        );
                    """.trimIndent())

                    st.execute("""
                        CREATE TABLE IF NOT EXISTS fahrzeug(
                            id TEXT PRIMARY KEY,
                            bezeichnung TEXT, kennzeichen TEXT, bemerkung TEXT
                        );
                    """.trimIndent())

                    st.execute("""
                        CREATE TABLE IF NOT EXISTS material(
                            id TEXT PRIMARY KEY,
                            bezeichnung TEXT, bemerkung TEXT
                        );
                    """.trimIndent())

                    st.execute("""
                        CREATE TABLE IF NOT EXISTS schicht_person(
                            schicht_id TEXT, person_id TEXT,
                            PRIMARY KEY(schicht_id, person_id),
                            FOREIGN KEY(schicht_id) REFERENCES schicht(id),
                            FOREIGN KEY(person_id)  REFERENCES person(id)
                        );
                    """.trimIndent())

                    st.execute("""
                        CREATE TABLE IF NOT EXISTS schicht_fahrzeug(
                            schicht_id TEXT, fahrzeug_id TEXT,
                            PRIMARY KEY(schicht_id, fahrzeug_id),
                            FOREIGN KEY(schicht_id)  REFERENCES schicht(id),
                            FOREIGN KEY(fahrzeug_id) REFERENCES fahrzeug(id)
                        );
                    """.trimIndent())

                    st.execute("""
                        CREATE TABLE IF NOT EXISTS schicht_material(
                            schicht_id TEXT, material_id TEXT,
                            PRIMARY KEY(schicht_id, material_id),
                            FOREIGN KEY(schicht_id)  REFERENCES schicht(id),
                            FOREIGN KEY(material_id) REFERENCES material(id)
                        );
                    """.trimIndent())

                    // Version auf 1 setzen
                    conn.createStatement().execute("PRAGMA user_version = 1;")
                }

                // Migration auf Version 2: position-Spalte in person
                if (version < 2) {
                    st.execute("ALTER TABLE person ADD COLUMN position INTEGER DEFAULT 0;")
                    st.execute("UPDATE person SET position = rowid;") // initiale Reihenfolge
                    conn.createStatement().execute("PRAGMA user_version = 2;")
                }

                // Migration auf Version 3: Soft-Delete-Flag in person
                if (version < 3) {
                    st.execute("ALTER TABLE person ADD COLUMN is_deleted INTEGER DEFAULT 0;")
                    conn.createStatement().execute("PRAGMA user_version = 3;")
                }
                if (version < 4) {
                    // 1) Archiv-Tabelle anlegen
                    st.execute("""
        CREATE TABLE IF NOT EXISTS person_archive (
            id TEXT PRIMARY KEY,
            vorname TEXT,
            name TEXT,
            firma TEXT,
            bemerkung TEXT,
            position INTEGER DEFAULT 0,
            is_deleted INTEGER DEFAULT 1
        );
    """.trimIndent())

                    // 2) Trigger, der vor jedem DELETE aus person die Zeile in person_archive kopiert
                    st.execute("""
        CREATE TRIGGER IF NOT EXISTS trg_person_archive
        BEFORE DELETE ON person
        FOR EACH ROW
        BEGIN
            INSERT OR REPLACE INTO person_archive
            (id, vorname, name, firma, bemerkung, position, is_deleted)
            VALUES
            (OLD.id, OLD.vorname, OLD.name, OLD.firma, OLD.bemerkung, OLD.position, 1);
        END;
    """.trimIndent())

                    // 3) Versionsnummer hochsetzen
                    conn.createStatement().execute("PRAGMA user_version = 4;")
                }

                // Migration auf Version 5: Tabelle für Stundenzettel
                if (version < 5) {
                    st.execute("""
        CREATE TABLE IF NOT EXISTS stundenzettel(
            id          TEXT PRIMARY KEY,
            auftrag_id  TEXT NOT NULL,
            startDatum  TEXT,
            endDatum    TEXT,
            pfad        TEXT,
            FOREIGN KEY(auftrag_id) REFERENCES auftrag(id)
        );
    """.trimIndent())
                    conn.createStatement().execute("PRAGMA user_version = 5;")
                }

            }
        }
    }

    /** CRUD für Person inkl. Positions-Persistenz */
    fun insertPerson(p: Person) {
        // nächste Position ermitteln
        val nextPos = getConnection().use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT COALESCE(MAX(position), -1) + 1 FROM person;").use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
        val sql = "INSERT INTO person(id, vorname, name, firma, bemerkung, position) VALUES(?,?,?,?,?,?)"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, p.id)
                stmt.setString(2, p.vorname)
                stmt.setString(3, p.name)
                stmt.setString(4, p.firma)
                stmt.setString(5, p.bemerkung)
                stmt.setInt(6, nextPos)
                stmt.executeUpdate()
            }
        }
    }

    fun insertStundenzettel(auftragId: String, z: Stundenzettel) {
        val sql = """
        INSERT INTO stundenzettel(id, auftrag_id, startDatum, endDatum, pfad)
        VALUES(?,?,?,?,?);
    """.trimIndent()
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, z.id)
                stmt.setString(2, auftragId)
                stmt.setString(3, z.startDatum?.toString())
                stmt.setString(4, z.endDatum?.toString())
                stmt.setString(5, z.pfad)
                stmt.executeUpdate()
            }
        }
    }

    fun getStundenzettelForAuftrag(auftragId: String): List<Stundenzettel> {
        val list = mutableListOf<Stundenzettel>()
        getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT id, startDatum, endDatum, pfad FROM stundenzettel WHERE auftrag_id = ?"
            ).use { stmt ->
                stmt.setString(1, auftragId)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list += Stundenzettel(
                        id         = rs.getString("id"),
                        startDatum = rs.getString("startDatum")?.let(LocalDateTime::parse),
                        endDatum   = rs.getString("endDatum")  ?.let(LocalDateTime::parse),
                        pfad       = rs.getString("pfad")
                    )
                }
            }
        }
        return list
    }

    fun updatePerson(id: String, p: Person) {
        val sql = "UPDATE person SET vorname=?, name=?, firma=?, bemerkung=? WHERE id=?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, p.vorname)
                stmt.setString(2, p.name)
                stmt.setString(3, p.firma)
                stmt.setString(4, p.bemerkung)
                stmt.setString(5, id)
                stmt.executeUpdate()
            }
        }
    }

    fun updatePersonPosition(id: String, position: Int) {
        val sql = "UPDATE person SET position = ? WHERE id = ?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, position)
                stmt.setString(2, id)
                stmt.executeUpdate()
            }
        }
    }

    fun deletePerson(id: String) {
        getConnection().use { conn ->
            conn.prepareStatement("UPDATE person SET is_deleted = 1 WHERE id = ?").use {
                it.setString(1, id); it.executeUpdate()
            }
        }
    }

    fun getAllPerson(): List<Person> {
        val list = mutableListOf<Person>()
        getConnection().use { conn ->
            conn.prepareStatement("""
                SELECT id, vorname, name, firma, bemerkung, COALESCE(position,0) AS position
                FROM person
                WHERE is_deleted = 0
                ORDER BY position ASC
                """.trimIndent()
            ).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list += Person(
                        id        = rs.getString("id"),
                        vorname   = rs.getString("vorname"),
                        name      = rs.getString("name"),
                        firma     = rs.getString("firma"),
                        bemerkung = rs.getString("bemerkung"),
                        position  = rs.getInt("position")
                    )
                }
            }
        }
        return list
    }

    // Analog: CRUD für Fahrzeug
    fun insertFahrzeug(f: Fahrzeug) {
        val sql = "INSERT INTO fahrzeug(id, bezeichnung, kennzeichen, bemerkung) VALUES(?,?,?,?)"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, f.id)
                stmt.setString(2, f.bezeichnung)
                stmt.setString(3, f.kennzeichen)
                stmt.setString(4, f.bemerkung)
                stmt.executeUpdate()
            }
        }
    }

    fun updateFahrzeug(id: String, f: Fahrzeug) {
        val sql = "UPDATE fahrzeug SET bezeichnung=?, kennzeichen=?, bemerkung=? WHERE id=?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, f.bezeichnung)
                stmt.setString(2, f.kennzeichen)
                stmt.setString(3, f.bemerkung)
                stmt.setString(4, id)
                stmt.executeUpdate()
            }
        }
    }

    fun deleteFahrzeug(id: String) {
        getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM fahrzeug WHERE id=?").use {
                it.setString(1, id); it.executeUpdate()
            }
        }
    }

    fun getAllFahrzeug(): List<Fahrzeug> {
        val list = mutableListOf<Fahrzeug>()
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM fahrzeug").use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list += Fahrzeug(
                        id = rs.getString("id"),
                        bezeichnung = rs.getString("bezeichnung"),
                        kennzeichen = rs.getString("kennzeichen"),
                        bemerkung = rs.getString("bemerkung")
                    )
                }
            }
        }
        return list
    }

    // CRUD für Material
    fun insertMaterial(m: Material) {
        val sql = "INSERT INTO material(id, bezeichnung, bemerkung) VALUES(?,?,?)"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, m.id)
                stmt.setString(2, m.bezeichnung)
                stmt.setString(3, m.bemerkung)
                stmt.executeUpdate()
            }
        }
    }

    fun updateMaterial(id: String, m: Material) {
        val sql = "UPDATE material SET bezeichnung=?, bemerkung=? WHERE id=?"
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, m.bezeichnung)
                stmt.setString(2, m.bemerkung)
                stmt.setString(3, id)
                stmt.executeUpdate()
            }
        }
    }

    fun deleteMaterial(id: String) {
        getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM material WHERE id=?").use {
                it.setString(1, id); it.executeUpdate()
            }
        }
    }

    fun getAllMaterial(): List<Material> {
        val list = mutableListOf<Material>()
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM material").use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list += Material(
                        id = rs.getString("id"),
                        bezeichnung = rs.getString("bezeichnung"),
                        bemerkung = rs.getString("bemerkung")
                    )
                }
            }
        }
        return list
    }

    // CRUD für Auftrag + Schichten
    fun insertAuftrag(a: Auftrag) {
        val sql = """
        INSERT INTO auftrag(
            id, sapANummer, startDatum, endDatum,
            ort, strecke, kmVon, kmBis,
            massnahme, bemerkung
        ) VALUES(?,?,?,?,?,?,?,?,?,?)
    """.trimIndent()

        getConnection().use { conn ->
            // 1) Auftrag selbst anlegen
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, a.id)
                stmt.setString(2, a.sapANummer)
                stmt.setString(3, a.startDatum?.toString())
                stmt.setString(4, a.endDatum?.toString())
                stmt.setString(5, a.ort)
                stmt.setString(6, a.strecke)
                stmt.setString(7, a.kmVon)
                stmt.setString(8, a.kmBis)
                stmt.setString(9, a.massnahme)
                stmt.setString(10, a.bemerkung)
                stmt.executeUpdate()
            }

            // 2) Alle Schichten einfügen
            a.schichten.orEmpty().forEach { schicht ->
                insertSchicht(conn, a.id, schicht)
            }

            // 3) Alle Stundenzettel einfügen
            a.stundenzettel.forEach { zettel ->
                insertStundenzettel(a.id, zettel)
            }
        }
    }


    private fun insertSchicht(conn: Connection, auftragId: String, s: Schicht) {
        val sql = """
        INSERT INTO schicht(
            id, auftrag_id, startDatum, endDatum,
            ort, strecke, kmVon, kmBis,
            massnahme, bemerkung,
            pausenZeit
        ) VALUES(?,?,?,?,?,?,?,?,?,?,?)
    """.trimIndent()

        // 1) Schicht selbst einfügen
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, s.id)
            stmt.setString(2, auftragId)
            stmt.setString(3, s.startDatum?.toString())
            stmt.setString(4, s.endDatum?.toString())
            stmt.setString(5, s.ort)
            stmt.setString(6, s.strecke)
            stmt.setString(7, s.kmVon)
            stmt.setString(8, s.kmBis)
            stmt.setString(9, s.massnahme)
            stmt.setString(10, s.bemerkung)
            stmt.setInt(11, s.pausenZeit)
            stmt.executeUpdate()
        }

        // 2) Mitarbeiter-Zuordnung
        s.mitarbeiter.orEmpty().forEach { p ->
            conn.prepareStatement(
                "INSERT OR IGNORE INTO schicht_person(schicht_id, person_id) VALUES(?,?)"
            ).use {
                it.setString(1, s.id)
                it.setString(2, p.id)
                it.executeUpdate()
            }
        }

        // 3) Fahrzeug-Zuordnung
        s.fahrzeug.orEmpty().forEach { f ->
            conn.prepareStatement(
                "INSERT OR IGNORE INTO schicht_fahrzeug(schicht_id, fahrzeug_id) VALUES(?,?)"
            ).use {
                it.setString(1, s.id)
                it.setString(2, f.id)
                it.executeUpdate()
            }
        }

        // 4) Material-Zuordnung
        s.material.orEmpty().forEach { m ->
            conn.prepareStatement(
                "INSERT OR IGNORE INTO schicht_material(schicht_id, material_id) VALUES(?,?)"
            ).use {
                it.setString(1, s.id)
                it.setString(2, m.id)
                it.executeUpdate()
            }
        }
    }



    fun updateAuftrag(a: Auftrag) {
        val sql = """
        UPDATE auftrag SET
            sapANummer = ?, startDatum = ?, endDatum = ?,
            ort        = ?, strecke    = ?, kmVon     = ?,
            kmBis      = ?, massnahme  = ?, bemerkung = ?
        WHERE id = ?
    """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, a.sapANummer)
                stmt.setString(2, a.startDatum?.toString())
                stmt.setString(3, a.endDatum?.toString())
                stmt.setString(4, a.ort)
                stmt.setString(5, a.strecke)
                stmt.setString(6, a.kmVon)
                stmt.setString(7, a.kmBis)
                stmt.setString(8, a.massnahme)
                stmt.setString(9, a.bemerkung)
                stmt.setString(10, a.id)
                stmt.executeUpdate()
            }

            // Alte Schichten entfernen
            deleteSchichtenByAuftrag(conn, a.id)
            // Alte Stundenzettel entfernen
            deleteStundenzettelByAuftrag(conn, a.id)

            // Neue Schichten einfügen
            a.schichten.orEmpty().forEach { schicht ->
                insertSchicht(conn, a.id, schicht)
            }
            // Neue Stundenzettel einfügen
            a.stundenzettel.forEach { zettel ->
                insertStundenzettel(a.id, zettel)
            }
        }
    }



    fun deleteAuftrag(id: String) {
        getConnection().use { conn ->
            // Schichten und ihre Relationen
            deleteSchichtenByAuftrag(conn, id)
            // Stundenzettel
            deleteStundenzettelByAuftrag(conn, id)
            // dann den Auftrag selbst
            conn.prepareStatement("DELETE FROM auftrag WHERE id=?").use {
                it.setString(1, id)
                it.executeUpdate()
            }
        }
    }


    private fun deleteSchichtenByAuftrag(conn: Connection, auftragId: String) {
        // Fremdschlüssel-Tabellen leeren
        conn.prepareStatement("DELETE FROM schicht_person WHERE schicht_id IN (SELECT id FROM schicht WHERE auftrag_id=?)").use {
            it.setString(1, auftragId); it.executeUpdate()
        }
        conn.prepareStatement("DELETE FROM schicht_fahrzeug WHERE schicht_id IN (SELECT id FROM schicht WHERE auftrag_id=?)").use {
            it.setString(1, auftragId); it.executeUpdate()
        }
        conn.prepareStatement("DELETE FROM schicht_material WHERE schicht_id IN (SELECT id FROM schicht WHERE auftrag_id=?)").use {
            it.setString(1, auftragId); it.executeUpdate()
        }
        conn.prepareStatement("DELETE FROM schicht WHERE auftrag_id=?").use {
            it.setString(1, auftragId); it.executeUpdate()
        }
    }

    fun updateSchicht(schichtId: String, s: Schicht) {
        getConnection().use { conn ->
            conn.prepareStatement(
                """
            UPDATE schicht SET
                startDatum  = ?, endDatum  = ?, ort = ?, strecke = ?,
                kmVon       = ?, kmBis    = ?, massnahme = ?, bemerkung = ?,
                pausenZeit  = ?                 -- 🆕  hier ergänzen
            WHERE id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setString(1,  s.startDatum?.toString())
                stmt.setString(2,  s.endDatum?.toString())
                stmt.setString(3,  s.ort)
                stmt.setString(4,  s.strecke)
                stmt.setString(5,  s.kmVon)
                stmt.setString(6,  s.kmBis)
                stmt.setString(7,  s.massnahme)
                stmt.setString(8,  s.bemerkung)
                stmt.setInt   (9,  s.pausenZeit)      // 🆕  Pause in Minuten
                stmt.setString(10, schichtId)         // Index um +1 verschoben
                stmt.executeUpdate()
            }
            // alte Verknüpfungen löschen
            conn.prepareStatement("DELETE FROM schicht_person WHERE schicht_id=?").use { it.setString(1, schichtId); it.executeUpdate() }
            conn.prepareStatement("DELETE FROM schicht_fahrzeug WHERE schicht_id=?").use { it.setString(1, schichtId); it.executeUpdate() }
            conn.prepareStatement("DELETE FROM schicht_material WHERE schicht_id=?").use { it.setString(1, schichtId); it.executeUpdate() }
            // neue Verknüpfungen
            s.mitarbeiter?.forEach { rel ->
                conn.prepareStatement("INSERT OR IGNORE INTO schicht_person(schicht_id, person_id) VALUES(?,?)").use {
                    it.setString(1, schichtId); it.setString(2, rel.id); it.executeUpdate()
                }
            }
            s.fahrzeug?.forEach { rel ->
                conn.prepareStatement("INSERT OR IGNORE INTO schicht_fahrzeug(schicht_id, fahrzeug_id) VALUES(?,?)").use {
                    it.setString(1, schichtId); it.setString(2, rel.id); it.executeUpdate()
                }
            }
            s.material?.forEach { rel ->
                conn.prepareStatement("INSERT OR IGNORE INTO schicht_material(schicht_id, material_id) VALUES(?,?)").use {
                    it.setString(1, schichtId); it.setString(2, rel.id); it.executeUpdate()
                }
            }
        }
    }
    private fun deleteStundenzettelByAuftrag(conn: Connection, auftragId: String) {
        conn.prepareStatement(
            "DELETE FROM stundenzettel WHERE auftrag_id = ?"
        ).use {
            it.setString(1, auftragId)
            it.executeUpdate()
        }
    }

    fun deleteSchicht(id: String) {
        getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM schicht_person WHERE schicht_id=?").use { it.setString(1, id); it.executeUpdate() }
            conn.prepareStatement("DELETE FROM schicht_fahrzeug WHERE schicht_id=?").use { it.setString(1, id); it.executeUpdate() }
            conn.prepareStatement("DELETE FROM schicht_material WHERE schicht_id=?").use { it.setString(1, id); it.executeUpdate() }
            conn.prepareStatement("DELETE FROM schicht WHERE id=?").use { it.setString(1, id); it.executeUpdate() }
        }
    }

    // ruft alle Aufträge mit zugehörigen Schichten aus der DB
    fun getAllAuftraege(): List<Auftrag> {
        val list = mutableListOf<Auftrag>()
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM auftrag").use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val id = rs.getString("id")
                    val sap = rs.getString("sapANummer")
                    val start = rs.getString("startDatum")?.let { LocalDateTime.parse(it) }
                    val end = rs.getString("endDatum")?.let { LocalDateTime.parse(it) }
                    val ort = rs.getString("ort")
                    val strecke = rs.getString("strecke")
                    val kmVon = rs.getString("kmVon")
                    val kmBis = rs.getString("kmBis")
                    val massnahme = rs.getString("massnahme")
                    val bemerkung = rs.getString("bemerkung")
                    // hole alle Schichten zu diesem Auftrag
                    val schichten = conn.getSchichtenForAuftrag(id)
                    list += Auftrag(
                        id        = id,
                        sapANummer= sap,
                        startDatum= start,
                        endDatum  = end,
                        ort       = ort,
                        strecke   = strecke,
                        kmVon     = kmVon,
                        kmBis     = kmBis,
                        massnahme = massnahme,
                        bemerkung = bemerkung,
                        schichten = schichten
                    )
                }
            }
        }
        return list
    }
}

private fun Connection.getPersons(schichtId: String): List<Person> =
    prepareStatement("""
        SELECT * FROM person p
        JOIN schicht_person sp ON sp.person_id = p.id
        WHERE sp.schicht_id = ?
        UNION ALL
        SELECT * FROM person_archive pa
        JOIN schicht_person sp2 ON sp2.person_id = pa.id
        WHERE sp2.schicht_id = ?;
    """.trimIndent()).use { st ->
        st.setString(1, schichtId)
        st.setString(2, schichtId)
        st.executeQuery().let { rs ->
            buildList {
                while (rs.next()) add(
                    Person(
                        id        = rs.getString("id"),
                        vorname   = rs.getString("vorname"),
                        name      = rs.getString("name"),
                        firma     = rs.getString("firma"),
                        bemerkung = rs.getString("bemerkung")
                    )
                )
            }
        }
    }


private fun Connection.getFahrzeuge(schichtId: String): List<Fahrzeug> =
    prepareStatement("""
                SELECT f.* FROM fahrzeug f
                JOIN schicht_fahrzeug sf ON sf.fahrzeug_id = f.id
                WHERE sf.schicht_id = ?
            """).use { st ->
        st.setString(1, schichtId)
        st.executeQuery().let { rs ->
            buildList {
                while (rs.next()) add(
                    Fahrzeug(
                        id          = rs.getString("id"),
                        bezeichnung = rs.getString("bezeichnung"),
                        kennzeichen = rs.getString("kennzeichen"),
                        bemerkung   = rs.getString("bemerkung")
                    )
                )
            }
        }
    }

private fun Connection.getMaterial(schichtId: String): List<Material> =
    prepareStatement("""
                SELECT m.* FROM material m
                JOIN schicht_material sm ON sm.material_id = m.id
                WHERE sm.schicht_id = ?
            """).use { st ->
        st.setString(1, schichtId)
        st.executeQuery().let { rs ->
            buildList {
                while (rs.next()) add(
                    Material(
                        id          = rs.getString("id"),
                        bezeichnung = rs.getString("bezeichnung"),
                        bemerkung   = rs.getString("bemerkung")
                    )
                )
            }
        }
    }

/**  Lädt alle Schichten inkl. Relationen zu genau einem Auftrag  */
private fun Connection.getSchichtenForAuftrag(auftragId: String): List<Schicht> =
    buildList {
        prepareStatement("SELECT * FROM schicht WHERE auftrag_id = ?").use { st ->
            st.setString(1, auftragId)
            val rs = st.executeQuery()
            while (rs.next()) {

                /* 🆕  Pause auslesen */
                val pause = rs.getInt("pausenZeit")

                val schichtId = rs.getString("id")
                add(
                    Schicht(
                        id          = schichtId,
                        startDatum  = rs.getString("startDatum")?.let(LocalDateTime::parse),
                        endDatum    = rs.getString("endDatum")  ?.let(LocalDateTime::parse),
                        ort         = rs.getString("ort"),
                        strecke     = rs.getString("strecke"),
                        kmVon       = rs.getString("kmVon"),
                        kmBis       = rs.getString("kmBis"),
                        massnahme   = rs.getString("massnahme"),
                        bemerkung   = rs.getString("bemerkung"),

                        /* 🆕  hier einsetzen */
                        pausenZeit  = pause,

                        mitarbeiter = getPersons(schichtId),
                        fahrzeug    = getFahrzeuge(schichtId),
                        material    = getMaterial(schichtId)
                    )
                )
            }
        }
    }
