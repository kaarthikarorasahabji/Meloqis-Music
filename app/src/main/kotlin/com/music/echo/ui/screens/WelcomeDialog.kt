package iad1tya.echo.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import iad1tya.echo.music.R

@Composable
fun WelcomeDialog(
    startAtName: Boolean = false,
    onContinueAnonymous: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    var step by remember { mutableIntStateOf(if (startAtName) 3 else 0) }
    var anonymousName by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { step = 3 },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
            ) {
                OnboardingTopBar(
                    step = step,
                    onSkip = { step = 3 },
                )

                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (
                            slideInHorizontally(
                                animationSpec = tween(420),
                                initialOffsetX = { it * direction / 3 },
                            ) + fadeIn(tween(300))
                        ).togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(340),
                                targetOffsetX = { -it * direction / 4 },
                            ) + fadeOut(tween(220))
                        ).using(SizeTransform(clip = false))
                    },
                    label = "meloqis_onboarding_step",
                    modifier = Modifier.weight(1f),
                ) { currentStep ->
                    when (currentStep) {
                        0 -> WelcomeStep()
                        1 -> SignInGuideStep()
                        2 -> AccountChoiceStep()
                        else -> AnonymousNameStep(
                            name = anonymousName,
                            onNameChange = { anonymousName = it },
                        )
                    }
                }

                when (step) {
                    0 -> {
                        PrimaryOnboardingButton(
                            text = stringResource(R.string.onboarding_begin),
                            onClick = { step = 1 },
                        )
                    }

                    1 -> {
                        PrimaryOnboardingButton(
                            text = stringResource(R.string.onboarding_choose_access),
                            onClick = { step = 2 },
                        )
                        TextButton(
                            onClick = { step = 0 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        ) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    }

                    2 -> {
                        PrimaryOnboardingButton(
                            text = stringResource(R.string.onboarding_sign_in),
                            icon = R.drawable.person,
                            onClick = onSignIn,
                        )
                        OutlinedButton(
                            onClick = { step = 3 },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .height(56.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.music_note),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.onboarding_continue_anonymous),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        TextButton(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    }

                    else -> {
                        PrimaryOnboardingButton(
                            text = stringResource(R.string.onboarding_name_continue),
                            onClick = { onContinueAnonymous(anonymousName.trim()) },
                            enabled = anonymousName.trim().isNotEmpty(),
                        )
                        TextButton(
                            onClick = { step = 2 },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingTopBar(
    step: Int,
    onSkip: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            repeat(4) { index ->
                val width by animateDpAsState(
                    targetValue = if (step == index) 28.dp else 8.dp,
                    animationSpec = tween(280),
                    label = "onboarding_progress",
                )
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(8.dp)
                        .background(
                            color = if (index <= step) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f)
                            },
                            shape = CircleShape,
                        ),
                )
            }
        }
        if (step < 3) {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        MeloqisOrbit()
        Spacer(Modifier.height(38.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_to),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_welcome_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(0.86f),
        )
    }
}

@Composable
private fun MeloqisOrbit() {
    val orbit = rememberInfiniteTransition(label = "meloqis_orbit")
    val rotation by orbit.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "meloqis_orbit_rotation",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(190.dp),
    ) {
        Box(
            modifier = Modifier
                .size(176.dp)
                .graphicsLayer { rotationZ = rotation },
        ) {
            listOf(
                Alignment.TopCenter to R.drawable.search,
                Alignment.CenterEnd to R.drawable.playlist_play,
                Alignment.BottomCenter to R.drawable.sync,
            ).forEach { (alignment, icon) ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(alignment)
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            CircleShape,
                        ),
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = -rotation },
                    )
                }
            }
        }
        AsyncImage(
            model = R.mipmap.ic_launcher,
            contentDescription = null,
            modifier = Modifier
                .size(112.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        )
    }
}

@Composable
private fun SignInGuideStep() {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        OnboardingIcon(R.drawable.person)
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.onboarding_optional_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.onboarding_optional_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 26.dp),
        )
        GuideRow(
            number = "1",
            title = stringResource(R.string.onboarding_login_step_one),
            description = stringResource(R.string.onboarding_login_step_one_desc),
        )
        GuideRow(
            number = "2",
            title = stringResource(R.string.onboarding_login_step_two),
            description = stringResource(R.string.onboarding_login_step_two_desc),
        )
        GuideRow(
            number = "3",
            title = stringResource(R.string.onboarding_login_step_three),
            description = stringResource(R.string.onboarding_login_step_three_desc),
        )
    }
}

@Composable
private fun AccountChoiceStep() {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        OnboardingIcon(R.drawable.lock)
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.onboarding_choice_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.onboarding_choice_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
        )
        BenefitRow(
            icon = R.drawable.playlist_play,
            text = stringResource(R.string.onboarding_benefit_playlists),
        )
        BenefitRow(
            icon = R.drawable.sync,
            text = stringResource(R.string.onboarding_benefit_sync),
        )
        BenefitRow(
            icon = R.drawable.offline,
            text = stringResource(R.string.onboarding_benefit_anonymous),
        )
        Text(
            text = stringResource(R.string.onboarding_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun AnonymousNameStep(
    name: String,
    onNameChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        OnboardingIcon(R.drawable.person)
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.onboarding_name_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.onboarding_name_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { value ->
                onNameChange(value.take(32).filterNot { it == '\n' || it == '\r' })
            },
            label = { Text(stringResource(R.string.onboarding_name_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_name_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.onboarding_name_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun OnboardingIcon(icon: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(74.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), CircleShape),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun GuideRow(
    number: String,
    title: String,
    description: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        ) {
            Text(
                text = number,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun BenefitRow(
    icon: Int,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}

@Composable
private fun PrimaryOnboardingButton(
    text: String,
    icon: Int? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
        )
    }
}
