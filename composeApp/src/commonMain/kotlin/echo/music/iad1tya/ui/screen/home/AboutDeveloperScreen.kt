package echo.music.iad1tya.ui.screen.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import echo.music.iad1tya.ui.component.MeloqisFooter
import echo.music.iad1tya.ui.component.RippleIconButton
import echo.music.iad1tya.ui.component.rememberDeveloperSupportAction
import echo.music.iad1tya.ui.icon.ArrowBackIosNew
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.theme.LocalBatterySaver
import echomusic.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDeveloperScreen(navController: NavController, innerPadding: PaddingValues) {
    val uriHandler = LocalUriHandler.current
    val supportDeveloper = rememberDeveloperSupportAction()
    val reducedEffects = LocalBatterySaver.current
    val entrance = remember { Animatable(if (reducedEffects) 1f else 0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(if (reducedEffects) 0 else 240)) }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.about_developer), fontSize = 20.sp) },
                navigationIcon = {
                    RippleIconButton(
                        imageVector = echoIcons.ArrowBackIosNew,
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = { navController.popBackStack() },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).padding(horizontal = 28.dp, vertical = 28.dp)
                    .graphicsLayer {
                        alpha = entrance.value
                        translationY = (1f - entrance.value) * 16.dp.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(painterResource(Res.drawable.meloqis_logo), null, Modifier.size(96.dp))
                Text(stringResource(Res.string.app_name), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.developer_role), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Text(stringResource(Res.string.iad1tya_dev), fontSize = 24.sp, lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text(stringResource(Res.string.developer_message), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(Res.string.developer_support_message), style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)
                Button(onClick = supportDeveloper, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.support_coffee_button))
                }
                OutlinedButton(onClick = { uriHandler.openUri("https://axenoraai.in") }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.developer_website))
                }
                TextButton(onClick = { uriHandler.openUri("https://github.com/kaarthikarorasahabji/Meloqis-Music") }) {
                    Text(stringResource(Res.string.developer_source))
                }
            }
            MeloqisFooter(onClick = { uriHandler.openUri("https://axenoraai.in") })
        }
    }
}
