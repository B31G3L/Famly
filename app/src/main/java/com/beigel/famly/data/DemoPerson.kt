package com.beigel.famly.data

/**
 * Person im Demo-Datenbestand.
 *
 * Bewusst unabhängig vom produktiven [com.beigel.famly.data.model.Person] gehalten:
 * Der Demo-Bereich soll ohne Firebase und ohne Repository laufen. Der Übergang ist
 * eine reine Mapping-Funktion, siehe INTEGRATION.md.
 *
 * [parentIds] ist die Wahrheit fürs Baum-Layout, genau wie im produktiven Modell.
 * [partnerIds] wird beim Aufbau des Index beidseitig ergänzt, es reicht also, die
 * Partnerschaft an einer Person zu notieren.
 */
data class DemoPerson(
    val id: String,
    val firstName: String,
    val lastName: String,
    val birthName: String? = null,
    val isFemale: Boolean,
    /** ISO-Datum, yyyy-MM-dd. */
    val birth: String,
    /** ISO-Datum oder null, solange die Person lebt. */
    val death: String? = null,
    val birthPlace: String = "",
    val city: String = "",
    val job: String = "",
    val note: String = "",
    val partnerIds: List<String> = emptyList(),
    val parentIds: List<String> = emptyList()
) {
    val fullName: String get() = "$firstName $lastName"

    val initials: String
        get() = buildString {
            firstName.firstOrNull()?.let { append(it) }
            lastName.firstOrNull()?.let { append(it) }
        }.uppercase()

    val isDeceased: Boolean get() = death != null

    val birthYear: String get() = birth.take(4)

    val deathYear: String? get() = death?.take(4)
}

/**
 * Verwandtschaftsbeziehung relativ zur Ausgangsperson. Bewusst als Struktur statt
 * als fertiger String, damit die Beschriftung in der UI aus strings.xml kommt.
 */
sealed interface Relation {
    data object Self : Relation

    /** grad 1 = Mutter/Vater, 2 = Großmutter/-vater, 3 = Urgroßmutter/-vater, ... */
    data class Ancestor(val female: Boolean, val degree: Int) : Relation

    /** grad 1 = Tochter/Sohn, 2 = Enkelin/Enkel, 3 = Urenkelin/Urenkel, ... */
    data class Descendant(val female: Boolean, val degree: Int) : Relation

    data class Sibling(val female: Boolean) : Relation

    data class NieceNephew(val female: Boolean, val great: Boolean) : Relation

    data class AuntUncle(val female: Boolean, val great: Boolean) : Relation

    data class Cousin(val female: Boolean, val degree: Int, val removed: Int) : Relation

    data class Partner(val female: Boolean) : Relation

    /** Partner:in einer verwandten Person, z. B. "Bruders Partnerin". */
    data class InLaw(val of: Relation, val female: Boolean) : Relation

    data object Unrelated : Relation
}
