package com.beigel.famly.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.beigel.famly.ui.components.FamlyAvatar
import com.beigel.famly.ui.theme.FamlyBackground
import com.beigel.famly.ui.theme.FamlyDivider
import com.beigel.famly.ui.theme.FamlyTextSecondary
import com.beigel.famly.ui.theme.FamlyWhite

data class ProfileMenuEntry(val label: String, val onClick: () -> Unit)

@Composable
fun ProfileScreen(
    name: String,
    email: String,
    menuEntries: List<ProfileMenuEntry>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FamlyBackground)
    ) {
        Text(
            "Profil",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(22.dp, 22.dp, 22.dp, 14.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp, 8.dp, 22.dp, 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            FamlyAvatar(initial = name.take(1), size = 88, cornerRadius = 28)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(email, style = MaterialTheme.typography.bodyMedium, color = FamlyTextSecondary)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(FamlyWhite)
            ) {
                menuEntries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = entry.onClick)
                            .padding(18.dp, 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entry.label, style = MaterialTheme.typography.bodyLarge)
                        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = FamlyTextSecondary)
                    }
                    if (index != menuEntries.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = FamlyDivider)
                    }
                }
            }
        }
    }
}
