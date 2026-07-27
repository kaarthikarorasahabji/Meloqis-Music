# Meloqis Music Privacy Policy

Last updated: 2026-07-27

This draft applies to Meloqis Music, operated by Kaarthik Dass Arora through
Axenora AI. It must be reviewed and completed with the operator's legal address,
support email, production services, and retention periods before public release.

## Data on the device

Meloqis may store settings, listening history, playlists, cached media,
downloads, account identifiers, and integration tokens on the device. Android
cloud backup is disabled for the Meloqis app. Clearing app data or uninstalling
the app removes app-private local data, subject to files the user exported
elsewhere.

## Optional permissions

- **Display over other apps:** used only when the user enables Meloqis Now
  Capsule. The capsule displays and controls Meloqis playback only.
- **Microphone:** used only for music-recognition features initiated by the user.
- **Notifications:** used for media playback and related app activity.
- **Media/audio access:** used to find or play audio selected by the user.
- **Nearby/Bluetooth:** used for supported audio-device behavior.

Meloqis Now Capsule does not require notification access or an accessibility
service and does not inspect other apps.

## Network services and integrations

When a user chooses a network feature, the app may send requests to the selected
provider. Examples in the source include music, artwork, lyrics, recognition,
Discord, Spotify, Last.fm, ListenBrainz, and casting services. Those providers
receive network information such as IP address and the data required for the
request, and their own privacy policies apply.

Only services explicitly enabled and rights-cleared by the Meloqis operator may
be included in a public release. Upstream credentials and services are not
Meloqis services.

## Accounts and tokens

Supported sign-ins must use the provider's official authorization flow. Tokens
are used only to perform actions requested through that integration. Meloqis
does not intentionally collect passwords. Production releases must use secure
token storage and an Axenora-controlled backend where a confidential OAuth
client is required.

## Diagnostics

The production policy must identify any crash-reporting or analytics provider,
the fields sent, legal basis, retention period, and opt-out controls. A release
must not claim that no telemetry exists unless the shipped build has been
verified accordingly.

## Children, deletion, and contact

Age limits, regional rights, deletion request handling, and the privacy contact
will be added before public release. Until those details and the production
service inventory are complete, Meloqis is a development build and should not be
offered as a global consumer service.

Official website: https://axenoraai.in

Meloqis product website: https://meloqis.axenoraai.in

Developed with ❤️ by Kaarthik Dass Arora
