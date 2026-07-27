package com.beigel.famly.ui.theme

import androidx.compose.ui.graphics.Color

// Lila/Cream-Palette aus dem neuen Design-Handoff ("Famly_dc")
// Hinweis: Die Variablennamen (u. a. "Petrol") stammen noch aus der alten
// Palette und wurden hier bewusst NICHT umbenannt, um Breaking Changes in
// allen abhängigen Screens/Components zu vermeiden. Wer will, kann sie in
// einem separaten Schritt sauber umbenennen (z. B. FamlyPetrolPrimary ->
// FamlyPrimary).
val FamlyBackground = Color(0xFFFDF9F4)
val FamlyPetrolPrimary = Color(0xFF6C4FD6)       // Primärfarbe (vormals Petrol, jetzt Lila)
val FamlyPetrolPrimaryHover = Color(0xFF5A3FC0)
val FamlyTextPrimary = Color(0xFF2C2A35)
val FamlyTextSecondary = Color(0xFF8A8394)
val FamlyBodyText = Color(0xFF6B6478)
val FamlyCardBorder = Color(0xFFEEE5D8)
val FamlySurfaceTint = Color(0xFFEFE7FB)
val FamlyDivider = Color(0xFFEEE5D8)
val FamlyOutline = Color(0xFFA39CAE)
val FamlyChipBackground = Color(0xFFEFE7FB)
val FamlyChipText = Color(0xFF6C4FD6)
val FamlyInputBorder = Color(0xFFEEE5D8)
val FamlyDashedBorder = Color(0xFFD8CDBF)
val FamlyIconBackground = Color(0xFFF1ECE3)
val FamlyInactiveDot = Color(0xFFD8CDBF)
val FamlyTreeLine = Color(0xFFD8CDBF)
val FamlyStatusAlive = Color(0xFF21B8A4)
val FamlyLabelMuted = Color(0xFF7A728C)
val FamlySurfaceLight = Color(0xFFEFE7FB)
val FamlyWhite = Color(0xFFFFFFFF)

// Akzentfarbe für Highlights/FAB/Primärbutton mit Orange-Ton
val FamlyAccentOrange = Color(0xFFFF8A65)
val FamlyAccentYellow = Color(0xFFFFCA3A)

// Header-Gradient auf dem Dashboard
val FamlyHeaderGradientStart = Color(0xFF7D5CE0)
val FamlyHeaderGradientEnd = Color(0xFF5F3FC9)

// Generationsfarben für den Stammbaum-Canvas, in Reihenfolge
// Urgroßeltern -> Großeltern -> Eltern -> Kinder (zyklisch für tiefere Bäume)
val FamlyGenColors = listOf(
    Color(0xFF9B6BDE), // Urgroßeltern
    Color(0xFF4D8CF0), // Großeltern
    Color(0xFF21B8A4), // Eltern
    Color(0xFFFF8A65)  // Kinder
)

// Avatar-Akzentfarben für Familienmitglieder (AvatarAccent-Enum)
val FamlyAvatarYellow = Color(0xFFFFCA3A)
val FamlyAvatarOrange = Color(0xFFFF8A65)
val FamlyAvatarGreen = Color(0xFF21B8A4)
val FamlyAvatarPetrol = Color(0xFF4D8CF0)
val FamlyAvatarPurple = Color(0xFF9B6BDE)
