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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beigel.famly.data.model.Person
import com.beigel.famly.data.model.RelationType
import com.beigel.famly.ui.theme.FamlyDashedBorder
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyTextSecondary
import com.beigel.famly.ui.theme.FamlyWhite
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class PersonFormResult(
    val name: String,
    val birthDate: String,
    val birthPlace: String,
    val isDeceased: Boolean,
    val deathDate: String,
    val bio: String,
    val relationType: RelationType?
)

private val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

@Composable
fun AddPersonScreen(
    existingPerson: Person?,
    relativeOf: Person? = null,
    onClose: () -> Unit,
    onSave: (PersonFormResult) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(existingPerson?.name.orEmpty()) }
    var birthDateText by remember { mutableStateOf(existingPerson?.birthDate.orEmpty()) }
    var birthPlace by remember { mutableStateOf(existingPerson?.birthPlace.orEmpty()) }
    var bio by remember { mutableStateOf(existingPerson?.bio.orEmpty()) }
    var isDeceased by remember { mutableStateOf(existingPerson?.isDeceased ?: false) }
    var deathDateText by remember { mutableStateOf(existingPerson?.deathDate.orEmpty()) }
    var relationType by remember { mutableStateOf<RelationType?>(null) }
    var showBirthDatePicker by remember { mutableStateOf(false) }
    var showDeathDatePicker by remember { mutableStateOf(false) }

    val isEditMode = existingPerson != null
    val isRelativeFlow = !isEditMode && relativeOf != null
    val canSave = name.isNotBlank() && (!isRelativeFlow || relationType != null)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp, 18.dp, 18.dp, 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Schließen",
                tint = FamlyTextSecondary,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .size(20.dp)
            )
            Text(
                when {
                    isEditMode -> "Person bearbeiten"
                    isRelativeFlow -> "Verwandte:n hinzufügen"
                    else -> "Person hinzufügen"
                },
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                "Speichern",
                color = if (canSave) FamlyPetrolPrimary else FamlyTextSecondary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clickable(enabled = canSave) {
                    onSave(
                        PersonFormResult(
                            name = name.trim(),
                            birthDate = birthDateText,
                            birthPlace = birthPlace.trim(),
                            isDeceased = isDeceased,
                            deathDate = if (isDeceased) deathDateText else "",
                            bio = bio.trim(),
                            relationType = relationType
                        )
                    )
                }
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
                    Icon(
                        imageVector = Icons.Filled.AddAPhoto,
                        contentDescription = "Foto hinzufügen",
                        tint = FamlyTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (isRelativeFlow && relativeOf != null) {
                Column {
                    Text(
                        "Beziehung zu ${relativeOf.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FamlyTextSecondary
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RelationType.entries.forEach { type ->
                            RelationTypeChip(
                                label = type.label,
                                selected = relationType == type,
                                onClick = { relationType = type }
                            )
                        }
                    }
                }
            }

            FormField(label = "Name", value = name, onValueChange = { name = it }, placeholder = "z. B. Lena Müller")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(
                    label = "Geburtsdatum",
                    valueText = birthDateText,
                    onClick = { showBirthDatePicker = true },
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

            if (isDeceased) {
                DateField(
                    label = "Todesdatum",
                    valueText = deathDateText,
                    onClick = { showDeathDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
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

            if (isEditMode && onDelete != null) {
                Text(
                    "Person löschen",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDelete)
                        .padding(vertical = 8.dp)
                )
            }
        }
    }

    if (showBirthDatePicker) {
        DateOfBirthPickerDialog(
            initialText = birthDateText,
            onDismiss = { showBirthDatePicker = false },
            onConfirm = { millis ->
                birthDateText = displayFormat.format(millis)
                showBirthDatePicker = false
            }
        )
    }

    if (showDeathDatePicker) {
        DateOfBirthPickerDialog(
            initialText = deathDateText,
            onDismiss = { showDeathDatePicker = false },
            onConfirm = { millis ->
                deathDateText = displayFormat.format(millis)
                showDeathDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateOfBirthPickerDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initialMillis = remember(initialText) {
        runCatching { displayFormat.parse(initialText)?.time }.getOrNull()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let(onConfirm) ?: onDismiss() }
            ) {
                Text("Übernehmen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}

@Composable
private fun RelationTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) FamlyPetrolPrimary else FamlyWhite)
            .border(
                1.5.dp,
                if (selected) FamlyPetrolPrimary else FamlyDashedBorder,
                RoundedCornerShape(100.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp, 9.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) FamlyWhite else FamlyTextSecondary
        )
    }
}

@Composable
private fun DateField(
    label: String,
    valueText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FamlyTextSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, FamlyDashedBorder, RoundedCornerShape(16.dp))
                .background(FamlyWhite)
                .clickable(onClick = onClick)
                .padding(16.dp, 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                valueText.ifBlank { "TT.MM.JJJJ" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (valueText.isBlank()) FamlyTextSecondary else MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = FamlyTextSecondary,
                modifier = Modifier.size(18.dp)
            )
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
