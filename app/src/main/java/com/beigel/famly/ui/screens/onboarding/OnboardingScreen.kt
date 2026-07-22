package com.beigel.famly.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beigel.famly.ui.components.FamlyPrimaryButton
import com.beigel.famly.ui.theme.FamlyBackground
import com.beigel.famly.ui.theme.FamlySurfaceTint
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyTextSecondary

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FamlyBackground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .size(160.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(FamlySurfaceTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FamilyRestroom,
                    contentDescription = null,
                    tint = FamlyPetrolPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))

            Text(
                text = "Willkommen bei Offshoot",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 7.dp))
            Text(
                text = "Erstelle euren Familienstammbaum und lade eure Liebsten ein, ihn gemeinsam zu gestalten.",
                style = MaterialTheme.typography.bodyLarge,
                color = FamlyTextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Box(modifier = Modifier.padding(28.dp, 24.dp, 28.dp, 44.dp)) {
            FamlyPrimaryButton(text = "Los geht's", onClick = onGetStarted)
        }
    }
}
