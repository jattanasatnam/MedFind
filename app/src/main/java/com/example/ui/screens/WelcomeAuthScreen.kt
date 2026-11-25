package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.auth.onGoogleSignInClicked
import com.example.ui.components.MedFindBrandLogo
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.theme.PharmacyHeroEnd
import com.example.ui.theme.PharmacyHeroStart
import com.example.ui.theme.StatusOutOfStockFg

@Composable
fun WelcomeAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val bgPrimary = MaterialTheme.colorScheme.primary
    val bgTeal = MaterialTheme.colorScheme.secondary

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            // Subtle floating translucent capsules in the background (inspired by Image 1 Splash)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val capsules = listOf(
                    Triple(Offset(size.width * 0.14f, size.height * 0.18f), -25f, 92f),
                    Triple(Offset(size.width * 0.84f, size.height * 0.12f), 32f, 110f),
                    Triple(Offset(size.width * 0.12f, size.height * 0.78f), 28f, 124f),
                    Triple(Offset(size.width * 0.82f, size.height * 0.84f), -22f, 134f),
                    Triple(Offset(size.width * 0.52f, size.height * 0.92f), 14f, 108f)
                )
                capsules.forEachIndexed { idx, (pt, angle, len) ->
                    val tint = if (idx % 2 == 0) bgPrimary.copy(alpha = 0.055f) else bgTeal.copy(alpha = 0.055f)
                    rotate(degrees = angle, pivot = pt) {
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(pt.x - len / 2f, pt.y - len * 0.24f),
                            size = Size(len, len * 0.48f),
                            cornerRadius = CornerRadius(len * 0.24f, len * 0.24f)
                        )
                        drawRoundRect(
                            color = tint.copy(alpha = 0.09f),
                            topLeft = Offset(pt.x - len / 2f, pt.y - len * 0.24f),
                            size = Size(len, len * 0.48f),
                            cornerRadius = CornerRadius(len * 0.24f, len * 0.24f),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Centered Brand Mark & Wordmark (inspired by Image 1 Left phone)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.size(84.dp)
                ) {
                    MedFindBrandLogo(modifier = Modifier.padding(10.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Check pharmacy-reported stock before you travel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Dual Bold Quick Highlight Cards (inspired by Image 4 Left phone)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WelcomeHighlightTile(
                        title = "Check Medicine Availability",
                        subtitle = "Freshness badges & live store reports",
                        icon = Icons.Outlined.Medication,
                        gradient = Brush.linearGradient(listOf(PharmacyHeroStart, PharmacyHeroEnd)),
                        modifier = Modifier.weight(1f)
                    )
                    WelcomeHighlightTile(
                        title = "Nearby Verified Pharmacies",
                        subtitle = "Direct call & Google Maps routes",
                        icon = Icons.Outlined.LocalPharmacy,
                        gradient = Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OnboardingFeatureRow(
                            icon = Icons.Outlined.Schedule,
                            title = "Visible Stock Freshness",
                            subtitle = "See exact update timestamps—never stale claims presented as guarantees."
                        )
                        OnboardingFeatureRow(
                            icon = Icons.Outlined.LocationOn,
                            title = "Privacy-First Area & Map Search",
                            subtitle = "Pick a locality manually or use GPS only when searching nearby."
                        )
                        OnboardingFeatureRow(
                            icon = Icons.Outlined.PhoneInTalk,
                            title = "Call & Navigate via Google Maps",
                            subtitle = "Confirm stock with the pharmacy before making the trip."
                        )
                        OnboardingFeatureRow(
                            icon = Icons.Outlined.VerifiedUser,
                            title = "Verified Role-Based Access",
                            subtitle = "Customers browse safely; medical stores apply via drug-license verification."
                        )
                    }
                }

                MedicalDisclaimerBanner()

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        border = BorderStroke(1.dp, StatusOutOfStockFg.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Signing in...")
                    } else {
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Text(
                    text = "Role permissions (Customer, Verified Pharmacy Partner, or Admin) are verified from server records upon sign-in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WelcomeHighlightTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.86f)
            )
        }
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
