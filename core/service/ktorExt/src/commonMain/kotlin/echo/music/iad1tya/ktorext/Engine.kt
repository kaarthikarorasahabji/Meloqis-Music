package echo.music.iad1tya.ktorext

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

expect fun getEngine(): HttpClientEngineFactory<HttpClientEngineConfig>