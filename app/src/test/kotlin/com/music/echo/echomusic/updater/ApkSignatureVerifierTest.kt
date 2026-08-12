package iad1tya.echo.music.echomusic.updater

import org.junit.Assert.assertEquals
import org.junit.Test

class ApkSignatureVerifierTest {
    @Test
    fun `sha256 uses stable lowercase encoding`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ApkSignatureVerifier.sha256("abc".toByteArray()),
        )
    }
}
