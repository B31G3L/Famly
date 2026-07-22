package com.beigel.famly.ui.screens.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.beigel.famly.data.model.FamilyMember
import com.beigel.famly.data.model.MemberStatus
import com.beigel.famly.ui.components.FamlyAvatar
import com.beigel.famly.ui.theme.FamlyAvatarOrange
import com.beigel.famly.ui.theme.FamlyBackground
import com.beigel.famly.ui.theme.FamlyIconBackground
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyTextSecondary
import com.beigel.famly.ui.theme.FamlyWhite

@Composable
fun InviteScreen(
    familyName: String,
    memberCount: Int,
    inviteCode: String,
    members: List<FamilyMember>,
    onBack: () -> Unit,
    onCopyCode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FamlyBackground)
    ) {
        Row(
            modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FamlyIconBackground)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Zurück", modifier = Modifier.size(16.dp))
            }
            Text("Liste teilen", style = MaterialTheme.typography.labelLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp, 26.dp, 22.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FamlyAvatarOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Park, contentDescription = null, tint = FamlyWhite)
                }
                Column {
                    Text(familyName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "$memberCount Mitglieder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FamlyTextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(FamlyWhite)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    .padding(18.dp, 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Einladungscode", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.padding(top = 2.dp))
                    Text(inviteCode, style = MaterialTheme.typography.bodyMedium, color = FamlyTextSecondary)
                }
                Text(
                    "Kopieren",
                    color = FamlyPetrolPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.clickable(onClick = onCopyCode)
                )
            }

            Column {
                Text(
                    "MITGLIEDER",
                    style = MaterialTheme.typography.labelSmall,
                    color = FamlyTextSecondary
                )
                Spacer(modifier = Modifier.padding(top = 10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    members.forEach { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                FamlyAvatar(initial = member.person.initial, accentType = member.person.accent, size = 40, cornerRadius = 14)
                                Text(member.person.name, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(
                                text = when (member.status) {
                                    MemberStatus.OWNER -> "Besitzer"
                                    MemberStatus.MEMBER -> "Mitglied"
                                    MemberStatus.PENDING -> "Ausstehend"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = FamlyTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
