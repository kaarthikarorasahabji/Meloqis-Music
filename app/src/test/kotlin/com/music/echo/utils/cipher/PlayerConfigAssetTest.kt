package iad1tya.echo.music.utils.cipher

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayerConfigAssetTest {
    @Test
    fun bundledAssetContainsCurrentAugustPlayerRotation() {
        val assetText = File("src/main/assets/player_configs.json").readText()
        val playerEntries = Regex("\\\"[a-f0-9]{8}\\\"\\s*:\\s*\\{").findAll(assetText).count()

        assertTrue(assetText.contains("\"schemaVersion\": 1"))
        assertTrue(playerEntries >= 278)
        assertTrue(assetText.contains("\"4ecb77ba\""))
        assertTrue(assetText.contains("\"sts\": 20683"))
    }
}
