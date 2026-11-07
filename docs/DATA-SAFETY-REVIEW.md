# Data safety preparation

This is a source-based inventory for the owner to review, not a completed Play Console declaration. Collection, sharing, purpose, retention and optionality must match the released app, Firebase settings, connected providers and the current Play definitions.

| Feature | Data to review | Destination / behavior |
| --- | --- | --- |
| Firebase Analytics | App interactions, device/app metadata, app-instance identifiers and inferred regional information | Firebase project configured in `androidApp/google-services.json`; confirm project ownership and settings |
| Firebase Crashlytics | Crash logs, diagnostics, device/app metadata, installation identifiers | Firebase; review whether exception messages include additional user data |
| Search, playback, artwork, lyrics | Searches, content identifiers, request metadata and IP address; connected-account information where used | Music and lyrics providers contacted directly |
| Local library | Playlists, listening history, cached/downloaded audio, preferences | Local storage; review backups, exports and any optional synchronization |
| Optional notifications | Firebase installation/registration identifiers and topic subscription; greeting receipt is stored locally | Firebase Cloud Messaging in the owner-controlled `empmetria` project; opt-in, with Android notification controls |
| Music Lights | Decoded playback samples for local spectrum analysis; display preferences | On-device only; no microphone, recording or upload; optional overlay permission |
| Optional Last.fm / Discord | Connected-account identifiers/tokens and listening activity | Provider scrobbling or presence, when enabled |
| Optional AI translation/recommendations | Related text, prompts, selected endpoint and provider credentials | User-selected AI provider |
| Support requests | Email address and any content supplied by user | Developer's Gmail support inbox |

Before completing the form:

1. Inspect the merged final bundle manifest and Firebase SDK documentation. The Play build removes advertising ID permission and disables advertising ID collection and personalization signals, but Analytics and Crashlytics remain present.
2. Determine which network transfers count as collection or sharing under Play's current definitions, including service-provider and user-initiated exceptions. Do not guess “no collection” from the absence of a custom backend.
3. Verify all network endpoints and transport behavior before claiming every transfer is encrypted. The inherited network configuration permits cleartext and optional custom AI endpoints are user-configurable.
4. Confirm no separate Meloqis account creation flow exists in the final build. Third-party sign-in is not the same as a developer-managed account. Revisit account-deletion requirements if account creation is added.
5. Confirm retention and deletion operations for the actual Firebase project, connected accounts and support mailbox. Do not promise deletion of diagnostic records that cannot be associated with a user.
6. Provide foreground service explanations and demonstration videos for floating Music Lights, media playback and user-requested downloads as requested in Play Console. The optional floating display must be user-started and easy to stop.
7. Review audience and content rating against the actual catalog; do not select a children's audience merely because the app plays music.

Public pages are prepared for:

- Privacy: `https://axenoraai.in/meloqis/privacy.html`
- Support: `https://axenoraai.in/meloqis/support.html`
- Deletion: `https://axenoraai.in/meloqis/delete-data.html`

They are files in the hosting package until deployed; adding a URL to the app does not make it live.
