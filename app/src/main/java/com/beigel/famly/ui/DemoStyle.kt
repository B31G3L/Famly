package com.beigel.famly.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.beigel.famly.R
import com.beigel.famly.data.DemoPerson
import com.beigel.famly.data.Relation
import com.beigel.famly.tree.EdgeStyle
import com.beigel.famly.tree.TreeLine
import java.time.LocalDate
import java.time.MonthDay
import java.time.Period
import java.time.temporal.ChronoUnit

/**
 * Eigene Palette statt MaterialTheme-Farben: Der Baum lebt davon, dass die
 * Linienfarben Bedeutung tragen (väterlich / mütterlich / Nachkommen /
 * angeheiratet). Die müssen unabhängig vom Theme stabil bleiben.
 */
object DemoColors {
    val Background = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF0F172A)
    val Text = Color(0xFF1E293B)
    val Muted = Color(0xFF64748B)
    val Faint = Color(0xFF94A3B8)
    val Divider = Color(0xFFE2E8F0)

    val Paternal = Color(0xFF059669)
    val PaternalBorder = Color(0xFFA7F3D0)
    val PaternalSoft = Color(0xFFD1FAE5)

    val Maternal = Color(0xFF7C3AED)
    val MaternalBorder = Color(0xFFDDD6FE)
    val MaternalSoft = Color(0xFFEDE9FE)

    val Descendant = Color(0xFFD97706)
    val DescendantBorder = Color(0xFFFDE68A)
    val DescendantSoft = Color(0xFFFEF3C7)

    val Neutral = Color(0xFF94A3B8)
}

/** Kartenfarben je Linie. */
data class CardStyle(
    val background: Color,
    val border: Color,
    val title: Color,
    val subtitle: Color,
    val avatarBackground: Color,
    val avatarText: Color
)

fun cardStyleFor(line: TreeLine, isEgo: Boolean): CardStyle = when {
    isEgo || line == TreeLine.EGO -> CardStyle(
        background = DemoColors.Ink,
        border = DemoColors.Ink,
        title = Color.White,
        subtitle = Color(0xFFCBD5E1),
        avatarBackground = Color(0x33FFFFFF),
        avatarText = Color.White
    )
    line == TreeLine.PATERNAL -> CardStyle(
        DemoColors.Surface, DemoColors.PaternalBorder, Color(0xFF022C22),
        DemoColors.Paternal, DemoColors.PaternalSoft, Color(0xFF065F46)
    )
    line == TreeLine.MATERNAL -> CardStyle(
        DemoColors.Surface, DemoColors.MaternalBorder, Color(0xFF2E1065),
        DemoColors.Maternal, DemoColors.MaternalSoft, Color(0xFF5B21B6)
    )
    line == TreeLine.DESCENDANT -> CardStyle(
        DemoColors.Surface, DemoColors.DescendantBorder, Color(0xFF451A03),
        DemoColors.Descendant, DemoColors.DescendantSoft, Color(0xFF92400E)
    )
    else -> CardStyle(
        DemoColors.Surface, DemoColors.Divider, DemoColors.Text,
        DemoColors.Muted, Color(0xFFF1F5F9), DemoColors.Muted
    )
}

fun edgeColor(style: EdgeStyle): Color = when (style) {
    EdgeStyle.COUPLE -> DemoColors.Faint
    EdgeStyle.EGO -> DemoColors.Ink
    EdgeStyle.DESCENDANT -> DemoColors.Descendant
    EdgeStyle.PATERNAL -> DemoColors.Paternal
    EdgeStyle.MATERNAL -> DemoColors.Maternal
    EdgeStyle.NEUTRAL -> DemoColors.Neutral
}

/** Avatarfarbe außerhalb des Baums: verstorben grau, sonst nach Geschlecht. */
fun avatarColors(person: DemoPerson): Pair<Color, Color> = when {
    person.isDeceased -> Color(0xFFF1F5F9) to DemoColors.Faint
    person.isFemale -> DemoColors.MaternalSoft to Color(0xFF5B21B6)
    else -> DemoColors.PaternalSoft to Color(0xFF065F46)
}

// ── Datum und Alter ─────────────────────────────────────────────────────────

fun DemoPerson.birthDate(): LocalDate = LocalDate.parse(birth)

fun DemoPerson.deathDate(): LocalDate? = death?.let { LocalDate.parse(it) }

fun formatIsoDate(iso: String): String {
    val date = LocalDate.parse(iso)
    return "%02d.%02d.%d".format(date.dayOfMonth, date.monthValue, date.year)
}

/** Alter heute, bei Verstorbenen das erreichte Alter. */
fun DemoPerson.ageYears(today: LocalDate = LocalDate.now()): Int =
    Period.between(birthDate(), deathDate() ?: today).years

fun DemoPerson.daysUntilBirthday(today: LocalDate = LocalDate.now()): Long {
    val birthday = MonthDay.from(birthDate())
    // 29.02. in Nicht-Schaltjahren fällt auf den 28.02.
    var next = birthday.atYear(today.year)
    if (next.isBefore(today)) next = birthday.atYear(today.year + 1)
    return ChronoUnit.DAYS.between(today, next)
}

fun DemoPerson.daysUntilMemorial(today: LocalDate = LocalDate.now()): Long {
    val date = deathDate() ?: return Long.MAX_VALUE
    var next = MonthDay.from(date).atYear(today.year)
    if (next.isBefore(today)) next = MonthDay.from(date).atYear(today.year + 1)
    return ChronoUnit.DAYS.between(today, next)
}

/** "1935 – 2012" bzw. "* 1992". */
fun DemoPerson.lifeSpan(): String =
    if (deathYear != null) "$birthYear – $deathYear" else "* $birthYear"

// ── Verwandtschaftsbezeichnung ──────────────────────────────────────────────

@Composable
fun relationLabel(relation: Relation): String = when (relation) {
    is Relation.Self -> stringResource(R.string.demo_rel_self)
    is Relation.Ancestor -> when (relation.degree) {
        1 -> stringResource(if (relation.female) R.string.demo_rel_mother else R.string.demo_rel_father)
        2 -> stringResource(if (relation.female) R.string.demo_rel_grandmother else R.string.demo_rel_grandfather)
        3 -> stringResource(if (relation.female) R.string.demo_rel_great_grandmother else R.string.demo_rel_great_grandfather)
        4 -> stringResource(if (relation.female) R.string.demo_rel_great2_grandmother else R.string.demo_rel_great2_grandfather)
        else -> stringResource(if (relation.female) R.string.demo_rel_ancestor_f else R.string.demo_rel_ancestor_m)
    }
    is Relation.Descendant -> when (relation.degree) {
        1 -> stringResource(if (relation.female) R.string.demo_rel_daughter else R.string.demo_rel_son)
        2 -> stringResource(if (relation.female) R.string.demo_rel_granddaughter else R.string.demo_rel_grandson)
        3 -> stringResource(if (relation.female) R.string.demo_rel_great_granddaughter else R.string.demo_rel_great_grandson)
        else -> stringResource(if (relation.female) R.string.demo_rel_descendant_f else R.string.demo_rel_descendant_m)
    }
    is Relation.Sibling ->
        stringResource(if (relation.female) R.string.demo_rel_sister else R.string.demo_rel_brother)
    is Relation.NieceNephew -> when {
        relation.great && relation.female -> stringResource(R.string.demo_rel_grandniece)
        relation.great -> stringResource(R.string.demo_rel_grandnephew)
        relation.female -> stringResource(R.string.demo_rel_niece)
        else -> stringResource(R.string.demo_rel_nephew)
    }
    is Relation.AuntUncle -> when {
        relation.great && relation.female -> stringResource(R.string.demo_rel_greataunt)
        relation.great -> stringResource(R.string.demo_rel_greatuncle)
        relation.female -> stringResource(R.string.demo_rel_aunt)
        else -> stringResource(R.string.demo_rel_uncle)
    }
    is Relation.Cousin -> {
        val base = stringResource(
            if (relation.female) R.string.demo_rel_cousin_f else R.string.demo_rel_cousin_m,
            relation.degree
        )
        if (relation.removed > 0) {
            base + stringResource(R.string.demo_rel_cousin_removed, relation.removed)
        } else {
            base
        }
    }
    is Relation.Partner ->
        stringResource(if (relation.female) R.string.demo_rel_partner_f else R.string.demo_rel_partner_m)
    is Relation.InLaw -> stringResource(
        if (relation.female) R.string.demo_rel_inlaw_f else R.string.demo_rel_inlaw_m,
        relationLabel(relation.of)
    )
    is Relation.Unrelated -> stringResource(R.string.demo_rel_unrelated)
}
