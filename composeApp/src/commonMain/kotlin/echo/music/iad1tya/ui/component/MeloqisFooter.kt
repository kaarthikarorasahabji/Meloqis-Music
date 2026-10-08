package echo.music.iad1tya.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.developed_credit
import echomusic.composeapp.generated.resources.about_developer
import org.jetbrains.compose.resources.stringResource

/** A quiet developer credit linking to the dedicated developer page. */
@Composable
fun MeloqisFooter(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val credit = stringResource(Res.string.developed_credit)
    val cs = MaterialTheme.colorScheme
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = credit,
            style =
                TextStyle(
                    color = cs.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                ),
        )
        Text(
            text = stringResource(Res.string.about_developer),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}
