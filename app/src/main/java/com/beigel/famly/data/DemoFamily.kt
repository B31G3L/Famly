package com.beigel.famly.data

/**
 * Dummy-Datenbestand: Familie Berger, 51 Personen über sechs Generationen
 * (Ururgroßeltern bis Kinder), inklusive Verstorbener, angeheirateter Linien
 * und bewusst offener Lücken (Personen ohne erfasste Eltern).
 */
object DemoFamily {

    const val DEFAULT_EGO_ID = "me"

    val people: List<DemoPerson> = listOf(
        // ── Ururgroßeltern ───────────────────────────────────────────────────
        DemoPerson(
            id = "u1", firstName = "Josef", lastName = "Berger", isFemale = false,
            birth = "1874-03-02", death = "1941-11-18", birthPlace = "Kuchen",
            job = "Landwirt", partnerIds = listOf("u2"),
            note = "Hof an der Fils, Hofübergabe 1928."
        ),
        DemoPerson(
            id = "u2", firstName = "Katharina", lastName = "Berger", birthName = "Lang",
            isFemale = true, birth = "1879-07-21", death = "1958-02-09", birthPlace = "Gingen"
        ),
        DemoPerson(
            id = "u3", firstName = "Anton", lastName = "Klein", isFemale = false,
            birth = "1878-05-14", death = "1935-08-03", birthPlace = "Donzdorf",
            job = "Steinmetz", partnerIds = listOf("u4")
        ),
        DemoPerson(
            id = "u4", firstName = "Rosina", lastName = "Klein", birthName = "Maier",
            isFemale = true, birth = "1882-01-30", death = "1961-06-17", birthPlace = "Donzdorf"
        ),
        DemoPerson(
            id = "u5", firstName = "Georg", lastName = "Hofmann", isFemale = false,
            birth = "1871-09-08", death = "1939-04-25", birthPlace = "Süßen",
            job = "Weber", partnerIds = listOf("u6")
        ),
        DemoPerson(
            id = "u6", firstName = "Barbara", lastName = "Hofmann", birthName = "Steiner",
            isFemale = true, birth = "1876-12-02", death = "1952-10-11", birthPlace = "Süßen"
        ),

        // ── Urgroßeltern ─────────────────────────────────────────────────────
        DemoPerson(
            id = "g1", firstName = "Friedrich", lastName = "Berger", isFemale = false,
            birth = "1905-06-11", death = "1978-01-27", birthPlace = "Kuchen",
            job = "Schreiner", partnerIds = listOf("g2"), parentIds = listOf("u1", "u2"),
            note = "Eigene Werkstatt in Geislingen ab 1936."
        ),
        DemoPerson(
            id = "g2", firstName = "Anna", lastName = "Berger", birthName = "Klein",
            isFemale = true, birth = "1908-09-23", death = "1990-03-14",
            birthPlace = "Donzdorf", parentIds = listOf("u3", "u4")
        ),
        DemoPerson(
            id = "g3", firstName = "Otto", lastName = "Schuster", isFemale = false,
            birth = "1910-02-19", death = "1985-07-08", birthPlace = "Geislingen",
            job = "Bäcker", partnerIds = listOf("g4")
        ),
        DemoPerson(
            id = "g4", firstName = "Marta", lastName = "Schuster", birthName = "Vogel",
            isFemale = true, birth = "1912-11-05", death = "1998-12-01", birthPlace = "Eybach"
        ),
        DemoPerson(
            id = "g5", firstName = "Wilhelm", lastName = "Hofmann", isFemale = false,
            birth = "1902-04-17", death = "1971-09-30", birthPlace = "Süßen",
            job = "Weber", partnerIds = listOf("g6"), parentIds = listOf("u5", "u6")
        ),
        DemoPerson(
            id = "g6", firstName = "Frieda", lastName = "Hofmann", birthName = "Bauer",
            isFemale = true, birth = "1906-08-12", death = "1988-05-22", birthPlace = "Salach"
        ),
        DemoPerson(
            id = "g7", firstName = "Heinrich", lastName = "Wagner", isFemale = false,
            birth = "1908-01-09", death = "1944-08-19", birthPlace = "Ulm",
            job = "Schlosser", partnerIds = listOf("g8"),
            note = "Gefallen in Nordfrankreich, keine Grabstelle bekannt."
        ),
        DemoPerson(
            id = "g8", firstName = "Luise", lastName = "Wagner", birthName = "Roth",
            isFemale = true, birth = "1911-05-27", death = "1996-11-03", birthPlace = "Ulm"
        ),
        DemoPerson(
            id = "g9", firstName = "Maria", lastName = "Vogt", birthName = "Berger",
            isFemale = true, birth = "1908-10-30", death = "1994-04-06", birthPlace = "Kuchen",
            partnerIds = listOf("g10"), parentIds = listOf("u1", "u2")
        ),
        DemoPerson(
            id = "g10", firstName = "Emil", lastName = "Vogt", isFemale = false,
            birth = "1904-07-14", death = "1975-02-28", birthPlace = "Amstetten",
            job = "Eisenbahner"
        ),

        // ── Großeltern ───────────────────────────────────────────────────────
        DemoPerson(
            id = "o1", firstName = "Werner", lastName = "Berger", isFemale = false,
            birth = "1935-05-04", death = "2012-10-21", birthPlace = "Geislingen",
            job = "Maschinenbauer", partnerIds = listOf("o2"), parentIds = listOf("g1", "g2"),
            note = "Hat den Stammbaum 1994 auf Karteikarten angefangen."
        ),
        DemoPerson(
            id = "o2", firstName = "Ingrid", lastName = "Berger", birthName = "Schuster",
            isFemale = true, birth = "1938-08-16", death = "2019-01-09",
            birthPlace = "Geislingen", job = "Verkäuferin", parentIds = listOf("g3", "g4")
        ),
        DemoPerson(
            id = "o3", firstName = "Karl", lastName = "Hofmann", isFemale = false,
            birth = "1934-03-27", death = "2001-06-12", birthPlace = "Süßen",
            job = "Lehrer", partnerIds = listOf("o4"), parentIds = listOf("g5", "g6")
        ),
        DemoPerson(
            id = "o4", firstName = "Elisabeth", lastName = "Hofmann", birthName = "Wagner",
            isFemale = true, birth = "1939-11-11", birthPlace = "Ulm", city = "Süßen",
            job = "Schneiderin", parentIds = listOf("g7", "g8"),
            note = "Älteste lebende Person im Baum."
        ),
        DemoPerson(
            id = "o5", firstName = "Hans", lastName = "Berger", isFemale = false,
            birth = "1940-12-06", death = "2015-03-30", birthPlace = "Geislingen",
            job = "Schreiner", partnerIds = listOf("o6"), parentIds = listOf("g1", "g2")
        ),
        DemoPerson(
            id = "o6", firstName = "Hilde", lastName = "Berger", birthName = "Fuchs",
            isFemale = true, birth = "1943-06-25", birthPlace = "Kuchen", city = "Kuchen"
        ),
        DemoPerson(
            id = "o7", firstName = "Renate", lastName = "Sauer", birthName = "Vogt",
            isFemale = true, birth = "1936-02-14", birthPlace = "Amstetten",
            city = "Amstetten", parentIds = listOf("g9", "g10")
        ),

        // ── Elterngeneration ─────────────────────────────────────────────────
        DemoPerson(
            id = "e1", firstName = "Michael", lastName = "Berger", isFemale = false,
            birth = "1963-07-19", birthPlace = "Geislingen", city = "Geislingen an der Steige",
            job = "Elektromeister", partnerIds = listOf("e2"), parentIds = listOf("o1", "o2")
        ),
        DemoPerson(
            id = "e2", firstName = "Petra", lastName = "Berger", birthName = "Hofmann",
            isFemale = true, birth = "1965-04-03", birthPlace = "Süßen",
            city = "Geislingen an der Steige", job = "Erzieherin", parentIds = listOf("o3", "o4")
        ),
        DemoPerson(
            id = "e3", firstName = "Andrea", lastName = "Fischer", birthName = "Berger",
            isFemale = true, birth = "1966-09-28", birthPlace = "Geislingen",
            city = "Göppingen", job = "Bankkauffrau", partnerIds = listOf("e4"),
            parentIds = listOf("o1", "o2")
        ),
        DemoPerson(
            id = "e4", firstName = "Stefan", lastName = "Fischer", isFemale = false,
            birth = "1964-02-11", birthPlace = "Göppingen", city = "Göppingen",
            job = "Speditionskaufmann"
        ),
        DemoPerson(
            id = "e5", firstName = "Thomas", lastName = "Hofmann", isFemale = false,
            birth = "1968-06-30", birthPlace = "Süßen", city = "Ulm", job = "Architekt",
            partnerIds = listOf("e6"), parentIds = listOf("o3", "o4")
        ),
        DemoPerson(
            id = "e6", firstName = "Sabine", lastName = "Hofmann", birthName = "Neumann",
            isFemale = true, birth = "1970-10-19", birthPlace = "Ulm", city = "Ulm",
            job = "Apothekerin"
        ),
        DemoPerson(
            id = "e7", firstName = "Christine", lastName = "Berger", isFemale = true,
            birth = "1971-12-24", death = "2019-05-08", birthPlace = "Geislingen",
            job = "Krankenschwester", parentIds = listOf("o1", "o2")
        ),
        DemoPerson(
            id = "e8", firstName = "Dieter", lastName = "Berger", isFemale = false,
            birth = "1968-01-15", birthPlace = "Geislingen", city = "Kuchen",
            job = "Schreiner", partnerIds = listOf("e9"), parentIds = listOf("o5", "o6")
        ),
        DemoPerson(
            id = "e9", firstName = "Gabi", lastName = "Berger", birthName = "Krämer",
            isFemale = true, birth = "1971-03-22", birthPlace = "Gingen", city = "Kuchen"
        ),
        DemoPerson(
            id = "k1", firstName = "Reinhold", lastName = "Kessler", isFemale = false,
            birth = "1962-05-09", birthPlace = "Eislingen", city = "Eislingen",
            job = "Schlosser", partnerIds = listOf("k2")
        ),
        DemoPerson(
            id = "k2", firstName = "Doris", lastName = "Kessler", birthName = "Bach",
            isFemale = true, birth = "1964-08-24", birthPlace = "Salach", city = "Eislingen",
            job = "Floristin"
        ),

        // ── Eigene Generation ────────────────────────────────────────────────
        DemoPerson(
            id = "me", firstName = "Lukas", lastName = "Berger", isFemale = false,
            birth = "1992-06-08", birthPlace = "Geislingen", city = "Geislingen an der Steige",
            job = "Softwareentwickler", partnerIds = listOf("m2"), parentIds = listOf("e1", "e2"),
            note = "Pflegt den Stammbaum seit 2024 digital."
        ),
        DemoPerson(
            id = "m2", firstName = "Sarah", lastName = "Berger", birthName = "Kessler",
            isFemale = true, birth = "1993-02-17", birthPlace = "Eislingen",
            city = "Geislingen an der Steige", job = "Physiotherapeutin",
            parentIds = listOf("k1", "k2")
        ),
        DemoPerson(
            id = "s1", firstName = "Julia", lastName = "Krause", birthName = "Berger",
            isFemale = true, birth = "1989-11-02", birthPlace = "Geislingen",
            city = "Stuttgart", job = "Grafikerin", partnerIds = listOf("s2"),
            parentIds = listOf("e1", "e2")
        ),
        DemoPerson(
            id = "s2", firstName = "Tobias", lastName = "Krause", isFemale = false,
            birth = "1987-04-14", birthPlace = "Esslingen", city = "Stuttgart",
            job = "Projektleiter"
        ),
        DemoPerson(
            id = "s3", firstName = "Felix", lastName = "Berger", isFemale = false,
            birth = "1996-08-21", birthPlace = "Geislingen", city = "Ulm",
            job = "Mechatroniker", partnerIds = listOf("s4"), parentIds = listOf("e1", "e2")
        ),
        DemoPerson(
            id = "s4", firstName = "Lena", lastName = "Weber", isFemale = true,
            birth = "1997-01-06", birthPlace = "Ulm", city = "Ulm", job = "Lehrerin"
        ),
        DemoPerson(
            id = "c1", firstName = "Kevin", lastName = "Fischer", isFemale = false,
            birth = "1994-03-18", birthPlace = "Göppingen", city = "Göppingen",
            job = "IT-Systemkaufmann", partnerIds = listOf("c2"), parentIds = listOf("e3", "e4")
        ),
        DemoPerson(
            id = "c2", firstName = "Nina", lastName = "Fischer", birthName = "Braun",
            isFemale = true, birth = "1995-07-29", birthPlace = "Göppingen", city = "Göppingen"
        ),
        DemoPerson(
            id = "c3", firstName = "Laura", lastName = "Fischer", isFemale = true,
            birth = "1997-05-13", birthPlace = "Göppingen", city = "München",
            job = "Ärztin", parentIds = listOf("e3", "e4")
        ),
        DemoPerson(
            id = "c4", firstName = "Marie", lastName = "Hofmann", isFemale = true,
            birth = "1999-09-04", birthPlace = "Ulm", city = "Ulm", job = "Studentin",
            parentIds = listOf("e5", "e6")
        ),
        DemoPerson(
            id = "c5", firstName = "Jan", lastName = "Berger", isFemale = false,
            birth = "1995-12-11", birthPlace = "Geislingen", city = "Kuchen",
            job = "Schreiner", parentIds = listOf("e8", "e9")
        ),

        // ── Kinder ───────────────────────────────────────────────────────────
        DemoPerson(
            id = "x1", firstName = "Emma", lastName = "Berger", isFemale = true,
            birth = "2019-03-12", birthPlace = "Geislingen",
            city = "Geislingen an der Steige", parentIds = listOf("me", "m2")
        ),
        DemoPerson(
            id = "x2", firstName = "Ben", lastName = "Berger", isFemale = false,
            birth = "2022-09-30", birthPlace = "Göppingen",
            city = "Geislingen an der Steige", parentIds = listOf("me", "m2")
        ),
        DemoPerson(
            id = "x3", firstName = "Mia", lastName = "Krause", isFemale = true,
            birth = "2015-06-19", birthPlace = "Stuttgart", city = "Stuttgart",
            parentIds = listOf("s1", "s2")
        ),
        DemoPerson(
            id = "x4", firstName = "Noah", lastName = "Krause", isFemale = false,
            birth = "2018-08-07", birthPlace = "Stuttgart", city = "Stuttgart",
            parentIds = listOf("s1", "s2")
        ),
        DemoPerson(
            id = "x5", firstName = "Theo", lastName = "Berger", isFemale = false,
            birth = "2024-04-02", birthPlace = "Ulm", city = "Ulm",
            parentIds = listOf("s3", "s4")
        ),
        DemoPerson(
            id = "x6", firstName = "Lias", lastName = "Fischer", isFemale = false,
            birth = "2021-10-25", birthPlace = "Göppingen", city = "Göppingen",
            parentIds = listOf("c1", "c2")
        )
    )
}
