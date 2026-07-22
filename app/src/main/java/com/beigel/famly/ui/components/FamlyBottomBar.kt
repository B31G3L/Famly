package com.beigel.famly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyTextSecondary
import com.beigel.famly.ui.theme.FamlyWhite

enum class FamlyBottomDestination(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.Home, Icons.Outlined.Home),
    TREE("tree", "Baum", Icons.Filled.Groups, Icons.Outlined.Groups),
    INVITE("invite", "Einladen", Icons.Filled.PersonAdd, Icons.Outlined.PersonAdd),
    PROFILE("profile", "Profil", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

@Composable
fun FamlyBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FamlyWhite)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FamlyBottomDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            Column(
                modifier = Modifier
                    .clickable { onNavigate(destination.route) }
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (selected) destination.filledIcon else destination.outlinedIcon,
                    contentDescription = destination.label,
                    tint = if (selected) FamlyPetrolPrimary else FamlyTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = destination.label,
                    fontSize = 11.sp,
                    color = if (selected) FamlyPetrolPrimary else FamlyTextSecondary
                )
            }
        }
    }
}
