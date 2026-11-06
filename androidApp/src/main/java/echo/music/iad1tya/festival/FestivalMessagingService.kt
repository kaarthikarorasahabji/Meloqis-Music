package echo.music.iad1tya.festival

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import echo.music.iad1tya.ui.component.FestivalEngine

class FestivalMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        FestivalEngine.receive(this, message.data)
    }
    override fun onNewToken(token: String) {
        // Firebase retains topic subscriptions across token refresh; startup also reconciles consent.
        if (FestivalEngine.enabled.value) com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic(FestivalEngine.TOPIC)
    }
}
