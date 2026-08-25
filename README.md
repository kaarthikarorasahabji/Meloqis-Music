<div align="center">
  <img src="assets/meloqis-logo.png" alt="Meloqis Music Logo" width="120" style="border-radius: 24px;"/>

  <h1>Meloqis Music</h1>

  <p><b>A beautiful, Apple Music-inspired Android player that paints itself with the colour of every song — with streaming, synced lyrics, offline playback, and living colour.</b></p>
  <p>
    <a href="https://github.com/kaarthikarorasahabji/Meloqis-Music-KMP/releases/latest">Download</a> •
    <a href="https://axenoraai.in">Axenora AI</a> •
    <a href="https://github.com/kaarthikarorasahabji/Meloqis-Music-KMP">GitHub</a>
  </p>
</div>

## Screenshots

<div align="center">
  <img src="Screenshots/HomeScreen.png" alt="Home Screen" width="18%" style="border-radius: 10px; margin: 5px;" />
  <img src="Screenshots/SearchPage.png" alt="Search Page" width="18%" style="border-radius: 10px; margin: 5px;" />
  <img src="Screenshots/MusicPage.png" alt="Music Player" width="18%" style="border-radius: 10px; margin: 5px;" />
  <img src="Screenshots/LyricsPage.png" alt="Lyrics Page" width="18%" style="border-radius: 10px; margin: 5px;" />
  <img src="Screenshots/LibraryPage.png" alt="Library Page" width="18%" style="border-radius: 10px; margin: 5px;" />
</div>

## Features

* **Living colour** — the whole interface re-tints itself in real time from the album art of whatever's playing.
* Apple Music-inspired design with smooth motion and an animated startup experience.
* High-quality audio streaming (up to 256kbps for supported accounts).
* Browse charts, podcasts, moods, and genres.
* Comprehensive search functionality across the music catalog.
* Playback data analytics and automated custom playlists.
* Video playback support (1080p with subtitles).
* Artificial Intelligence based song suggestions.
* Crossfade and gapless playback capabilities.
* Customizable application themes (Light, Dark, and dynamic colors).
* Sleep timer functionality.
* Android Auto integration for in-car listening.
* Support for Spotify Canvas visualizations.
* **In-app updates** — tap update inside the app and the newest release downloads and installs itself.

## Architecture

Meloqis Music is built utilizing a modern Android and Kotlin Multiplatform (KMP) architecture to ensure scalability, maintainability, and high performance.

* **Kotlin Multiplatform (KMP):** The core business logic, domain models, and data access layers are encapsulated within the `core` directory. This enables logic sharing across platforms and isolates critical services.
* **UI Layer:** The application interface is built entirely with Jetpack Compose, offering a reactive and declarative UI paradigm.
* **Media Playback:** Playback is handled by AndroidX Media3 (ExoPlayer), providing robust handling of audio streams, local caching, and gapless transitions.
* **Dependency Injection:** Koin is utilized for dependency injection, decoupling module lifecycles and simplifying testing.
* **Local Storage:** Room Database manages structured local data (playlists, favorites, cache metadata) while DataStore manages user preferences.
* **Modularization:** The project is strictly modularized by feature and layer (e.g., `:core:data`, `:core:domain`, `:core:media3`, `:core:service:spotify`, `:core:service:lyricsService`). This structure reduces build times and enforces clear boundary separations.

## Building from source

```bash
git clone https://github.com/kaarthikarorasahabji/Meloqis-Music-KMP.git
cd Meloqis-Music-KMP
./gradlew :androidApp:assembleDebug
```

The debug APKs are produced in `androidApp/build/outputs/apk/debug/`. Requires JDK 21 and the Android SDK.

## In-app updates

Meloqis Music checks the [Releases](https://github.com/kaarthikarorasahabji/Meloqis-Music-KMP/releases/latest) page for new versions. When an update is available, the in-app dialog downloads the release APK and launches the installer automatically — no store required.

> **Note:** For in-app updates to install successfully, every release APK must be signed with the **same signing key** as the version already installed on the device. Publish releases using a stable release keystore (see `androidApp` signing config), not the machine-local debug key.

## Installation

Download the latest pre-compiled APK from the [Releases Page](https://github.com/kaarthikarorasahabji/Meloqis-Music-KMP/releases/latest).

## Support

If you enjoy Meloqis Music, please consider supporting development:

<div align="center">
  <a href="https://intradeus.github.io/http-protocol-redirector/?r=upi://pay?pa=kaarthikdassarorasahabji@sbi&pn=Kaarthik%20Dass%20Arora%20Sahab%20Ji&cu=INR"><b>Support via UPI</b></a>
</div>

<div align="center">
  <br/>
  <code>UPI ID: kaarthikdassarorasahabji@sbi</code>
</div>

## Acknowledgements

Meloqis Music is an independent rebrand built on top of the open-source [Echo Music](https://github.com/EchoMusicApp/Echo-Music) project by Aditya ([@iad1tya](https://github.com/iad1tya)), which itself builds on the excellent [SimpMusic](https://github.com/maxrave-dev/SimpMusic) project by [maxrave-dev](https://github.com/maxrave-dev). Huge thanks to both projects for their outstanding open-source work, which forms the reliable foundation this project builds upon.

## Legal Disclaimer & Terms of Use

**1. Free & Open-Source**
Meloqis Music is a 100% free, open-source (FOSS) application built for educational purposes and personal use. It is not sold or monetized in any way — no ads, no premium features, no subscriptions, and no hidden fees.

**2. How It Works**
Meloqis Music functions as a specialized client that parses the publicly available content and APIs of YouTube and YouTube Music, displaying them in a custom interface. This ad-free experience is comparable to using a standard browser with an ad-blocking extension (like uBlock Origin) — it doesn't modify or bypass any content protections beyond that.

**3. Support Creators**
We respect the work of artists and content creators. Users are encouraged to subscribe to [YouTube Premium](https://www.youtube.com/premium) to directly support the creators they listen to. Meloqis Music is intended as a developer proof-of-concept, not as a way to reduce creator revenue.

**4. No Hosted Content**
Meloqis Music does not host, upload, or store any audio, video, or copyrighted media on its own servers. All content remains hosted on Google/YouTube's servers and is the property of its respective owners. The app simply streams publicly accessible links.

**5. User Responsibility**
This software is provided "AS IS," without warranty of any kind. Users are solely responsible for ensuring their use of the app complies with local copyright laws and the platform's Terms of Service. Since no media is hosted by us, we cannot process DMCA takedowns for audio/video content.

## License

Meloqis Music is licensed under the **GPL-3.0 License**, inherited from its upstream projects (Echo Music and SimpMusic). See the [LICENSE](LICENSE) file for details. In accordance with the GPL-3.0, the original copyright notices and attribution are preserved.

---

<div align="center">
  <sub>developed with ❤ by Kaarthik Dass Arora Sahab Ji</sub>
</div>
