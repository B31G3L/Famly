package com.beigel.famly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.beigel.famly.ui.FamlyDemoApp

/**
 * Farbschema für die Material-Komponenten (Textfeld-Fokus, Ripple, Sheet-Griff).
 * Die Farben des Baums stecken bewusst nicht hier, sondern in
 * [com.beigel.famly.ui.DemoColors] - dort tragen sie Bedeutung und dürfen sich
 * nicht mit dem Theme verschieben.
 */
private val FamlyLightColors = lightColorScheme(
    primary = Color(0xFF006469),
    onPrimary = Color.White,
    secondary = Color(0xFF005055),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF)
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Ab targetSdk 35 erzwingt Android Edge-to-Edge ohnehin; das Scaffold in
        // FamlyDemoApp verrechnet die System-Insets über sein innerPadding.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = FamlyLightColors) {
                FamlyDemoApp()
            }
        }
    }
}
