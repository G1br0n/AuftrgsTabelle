    package repository


    import models.*
    import java.sql.Connection
    import java.sql.DriverManager
    import java.time.LocalDateTime

    class AuftragRepository(
        private val dbPath: String = "jdbc:sqlite:auftrag.db"
    ) {

        /* ────────────── DB‑Initialisierung ────────────── */

        init { createTables() }

        private fun getConnection(): Connection = DriverManager.getConnection(dbPath)

        private fun createTables() {
            getConnection().use { c -> c.createStatement().use { st ->

                st.execute("""
                    CREATE TABLE IF NOT EXISTS auftrag(
                        id TEXT PRIMARY KEY,
                        sapANummer TEXT,
                        startDatum TEXT,
                        endDatum   TEXT,
                        ort TEXT, strecke TEXT, kmVon TEXT, kmBis TEXT,
                        massnahme TEXT, bemerkung TEXT
                    );
                """)


                st.execute("""
                    CREATE TABLE IF NOT EXISTS schicht(
                        id TEXT PRIMARY KEY,
                        auftrag_id TEXT,
                        startDatum TEXT,
                        endDatum   TEXT,
                        ort TEXT, strecke TEXT, kmVon TEXT, kmBis TEXT,
                        massnahme TEXT,
                        bemerkung TEXT,
    
                        /* neu: */
                        pausenZeit INTEGER DEFAULT 0,
    
                        FOREIGN KEY(auftrag_id) REFERENCES auftrag(id)
                    );
                """)

                st.execute("""
                    CREATE TABLE IF NOT EXISTS person(
                        id TEXT PRIMARY KEY,
                        vorname TEXT, name TEXT, firma TEXT, bemerkung TEXT
                    );
                """)

                st.execute("""
                    CREATE TABLE IF NOT EXISTS fahrzeug(
                        id TEXT PRIMARY KEY,
                        bezeichnung TEXT, kennzeichen TEXT, bemerkung TEXT
                    );
                """)

                st.execute("""
                    CREATE TABLE IF NOT EXISTS material(
                        id TEXT PRIMARY KEY,
                        bezeichnung TEXT, bemerkung TEXT
                    );
                """)

                st.execute("""
                    CREATE TABLE IF NOT EXISTS schicht_person(
                        schicht_id TEXT, person_id TEXT,
                        PRIMARY KEY(schicht_id, person_id),
                        FOREIGN KEY(schicht_id) REFERENCES schicht(id),
                        FOREIGN KEY(person_id)  REFERENCES person(id)
                    );
                """)

                st.execute("""
                    CREATE TABLE IF NOT EXISTS schicht_fahrzeug(
                        schicht_id TEXT, fahrzeug_id TEXT,
                        PRIMARY KEY(schicht_id, fahrzeug_id),
                        FOREIGN KEY(schicht_id)  REFERENCES schicht(id),
                        FOREIGN KEY(fahrzeug_id) REFERENCES fahrzeug(id)
                    );
                """)

                st.execute("""
                    CREATE TABLE IF NOT EXISTS schicht_material(
                        schicht_id TEXT, material_id TEXT,
                        PRIMARY KEY(schicht_id, material_id),
                        FOREIGN KEY(schicht_id)  REFERENCES schicht(id),
                        FOREIGN KEY(material_id) REFERENCES material(id)
                    );
                """)
            }}
        }


        // CRUD für Person
        fun insertPerson(p: Person) {
            val sql = "INSERT INTO person(id, vorname, name, firma, bemerkung) VALUES(?,?,?,?,?)"
            getConnection().use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, p.id)
                    stmt.setString(2, p.vorname)
                    stmt.setString(3, p.name)
                    stmt.setString(4, p.firma)
                    stmt.setString(5, p.bemerkung)
                    stmt.executeUpdate()
                }
            }
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

        fun deletePerson(id: String) {
            getConnection().use { conn ->
                conn.prepareStatement("DELETE FROM person WHERE id=?").use {
                    it.setString(1, id); it.executeUpdate()
                }
            }
        }

        fun getAllPerson(): List<Person> {
            val list = mutableListOf<Person>()
            getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM person").use { stmt ->
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        list += Person(
                            id = rs.getString("id"),
                            vorname = rs.getString("vorname"),
                            name = rs.getString("name"),
                            firma = rs.getString("firma"),
                            bemerkung = rs.getString("bemerkung")
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
                a.schichten?.forEach { schicht -> insertSchicht(conn, a.id, schicht) }
            }
        }

        private fun insertSchicht(conn: Connection, auftragId: String, s: Schicht) {
            val sql = """
            INSERT INTO schicht(
                id, auftrag_id, startDatum, endDatum,
                ort, strecke, kmVon, kmBis,
                massnahme, bemerkung,  /*  ← Komma! */
                pausenZeit             /*  neu   */
            ) VALUES(?,?,?,?,?,?,?,?,?,?,?)
        """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, s.id)
                stmt.setString(2, auftragId)
                stmt.setString(3, s.startDatum?.toString())
                stmt.setString(4, s.endDatum  ?.toString())
                stmt.setString(5, s.ort)
                stmt.setString(6, s.strecke)
                stmt.setString(7, s.kmVon)
                stmt.setString(8, s.kmBis)
                stmt.setString(9, s.massnahme)
                stmt.setString(10, s.bemerkung)
                stmt.setInt   (11, s.pausenZeit)        // neu
                stmt.executeUpdate()
            }
        }


        fun updateAuftrag(a: Auftrag) {
            val sql = """
                UPDATE auftrag SET
                    sapANummer=?, startDatum=?, endDatum=?,
                    ort=?, strecke=?, kmVon=?, kmBis=?,
                    massnahme=?, bemerkung=?
                WHERE id=?
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
                // Alte Schichten löschen
                deleteSchichtenByAuftrag(conn, a.id)
                // Neue Schichten einfügen
                a.schichten?.forEach { insertSchicht(conn, a.id, it) }
            }
        }

        fun deleteAuftrag(id: String) {
            getConnection().use { conn ->
                deleteSchichtenByAuftrag(conn, id)
                conn.prepareStatement("DELETE FROM auftrag WHERE id=?").use {
                    it.setString(1, id); it.executeUpdate()
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
                SELECT p.* FROM person p
                JOIN schicht_person sp ON sp.person_id = p.id
                WHERE sp.schicht_id = ?
            """).use { st ->
            st.setString(1, schichtId)
            st.executeQuery().let { rs ->
                buildList {
                    while (rs.next()) add(
                        Person(
                            id       = rs.getString("id"),
                            vorname  = rs.getString("vorname"),
                            name     = rs.getString("name"),
                            firma    = rs.getString("firma"),
                            bemerkung= rs.getString("bemerkung")
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
