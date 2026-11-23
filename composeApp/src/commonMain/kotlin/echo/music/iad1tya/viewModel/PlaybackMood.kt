package echo.music.iad1tya.viewModel

import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.commute
import echomusic.composeapp.generated.resources.energize
import echomusic.composeapp.generated.resources.feel_good
import echomusic.composeapp.generated.resources.focus
import echomusic.composeapp.generated.resources.party
import echomusic.composeapp.generated.resources.relax
import echomusic.composeapp.generated.resources.romance
import echomusic.composeapp.generated.resources.sad
import echomusic.composeapp.generated.resources.sleep
import echomusic.composeapp.generated.resources.workout
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_COMMUTE
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_ENERGIZE
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_FEEL_GOOD
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_FOCUS
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_PARTY
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_RELAX
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_ROMANCE
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_SAD
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_SLEEP
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_WORKOUT
import org.jetbrains.compose.resources.StringResource

internal enum class PlaybackMood(val label: StringResource, val endpoint: String) {
    Workout(Res.string.workout, HOME_PARAMS_WORKOUT),
    FeelGood(Res.string.feel_good, HOME_PARAMS_FEEL_GOOD),
    Energize(Res.string.energize, HOME_PARAMS_ENERGIZE),
    Relax(Res.string.relax, HOME_PARAMS_RELAX),
    Focus(Res.string.focus, HOME_PARAMS_FOCUS),
    Sleep(Res.string.sleep, HOME_PARAMS_SLEEP),
    Sad(Res.string.sad, HOME_PARAMS_SAD),
    Romance(Res.string.romance, HOME_PARAMS_ROMANCE),
    Party(Res.string.party, HOME_PARAMS_PARTY),
    Commute(Res.string.commute, HOME_PARAMS_COMMUTE),
}
