package com.fishlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FirstRunScreen(
    isViewAgain: Boolean = false,
    modifier: Modifier = Modifier,
    onGetStarted: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isViewAgain) "Done" else "Get Started",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Icon(
                imageVector = Icons.Default.Water,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Welcome to FishLog",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your private fishing journal for trips, catches, no-catches, maps, and patterns.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            WelcomeItem(
                icon = Icons.Default.CloudOff,
                title = "FishLog works offline",
                description = "Log trips, catches, and no-catches even when you do not have service."
            )
            
            WelcomeItem(
                icon = Icons.Default.DirectionsBoat,
                title = "Start a trip",
                description = "Track when and where you fish, then attach catches and no-catches to the trip."
            )
            
            WelcomeItem(
                icon = Icons.Default.CheckCircle,
                title = "Log catches and no-catches",
                description = "No-catch logs help FishLog learn what did not work, not just what did."
            )
            
            WelcomeItem(
                icon = Icons.Default.Map,
                title = "Use maps and insights later",
                description = "Review spots, patterns, bait, depth, water temperature, weather, moon, and timing."
            )
            
            WelcomeItem(
                icon = Icons.Default.Lock,
                title = "Your data stays yours",
                description = "Your data stays on this device unless you choose to enable cloud backup."
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WelcomeItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
