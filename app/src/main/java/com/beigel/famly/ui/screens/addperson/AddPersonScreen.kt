package com.beigel.famly.ui.screens.addperson

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beigel.famly.ui.theme.FamlyDashedBorder
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyTextSecondary
import com.beigel.famly.ui.theme.FamlyWhite

@Composable
fun AddPersonScreen(
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var birthPlace by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isDeceased by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp, 18.dp, 18.dp, 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Schließen",
                tint = FamlyTextSecondary,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .size(20.dp)
            )
            Text("Person hinzufügen", style = MaterialTheme.typography.labelLarge)
            Text(
                "Speichern",
                color = FamlyPetrolPrimary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clickable(onClick = onSave)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp, 22.dp, 22.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, FamlyDashedBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.AddAPhoto,
                        contentDescription = "Foto hinzufügen",
                        tint = FamlyTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            FormField(label = "Name", value = name, onValueChange = { name = it }, placeholder = "z. B. Lena Müller")
            FormField(
                label = "Beziehung zu dir",
                value = relation,
                onValueChange = { relation = it },
                placeholder = "z. B. Schwester"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(
                    label = "Geburtsdatum",
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    placeholder = "TT.MM.JJJJ",
                    modifier = Modifier.weight(1f)
                )
                FormField(
                    label = "Geburtsort",
                    value = birthPlace,
                    onValueChange = { birthPlace = it },
                    placeholder = "Stadt",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Verstorben", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = isDeceased,
                    onCheckedChange = { isDeceased = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = FamlyPetrolPrimary)
                )
            }

            FormField(
                label = "Kurzbiografie",
                value = bio,
                onValueChange = { bio = it },
                placeholder = "Ein paar Sätze über diese Person …",
                singleLine = false,
                minHeight = 90.dp
            )

            Column {
                Text(
                    "Verbinden mit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FamlyTextSecondary
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConnectionChip(text = "Mama")
                    ConnectionChip(text = "Papa")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .border(1.5.dp, FamlyDashedBorder, RoundedCornerShape(100.dp))
                            .padding(13.dp, 7.dp)
                    ) {
                        Text("+ Hinzufügen", style = MaterialTheme.typography.bodySmall, color = FamlyTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FamlyTextSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = singleLine,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = FamlyWhite,
                focusedContainerColor = FamlyWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (minHeight > 0.dp) minHeight else 0.dp)
        )
    }
}

@Composable
private fun ConnectionChip(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(com.beigel.famly.ui.theme.FamlyChipBackground)
            .padding(13.dp, 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = com.beigel.famly.ui.theme.FamlyChipText, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.padding(start = 4.dp))
        Text("×", color = com.beigel.famly.ui.theme.FamlyChipText, style = MaterialTheme.typography.bodySmall)
    }
}
