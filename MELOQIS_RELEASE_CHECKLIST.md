# Meloqis public-release checklist

This checklist is an engineering and launch control, not a substitute for advice
from a lawyer in each country where Meloqis operates.

## Required before any APK is published

- [ ] Publish the exact corresponding source for the APK under GPLv3.
- [ ] Retain `LICENSE`, `NOTICE.md`, upstream copyright history, and modification notices.
- [ ] Create a Meloqis GitHub repository and make `/source` on the website point to it.
- [ ] Complete a trademark search for “Meloqis” in launch countries.
- [ ] Replace all remaining Echo-branded artwork, screenshots, support links, and user-facing translations.
- [ ] Use an original Meloqis launcher icon and store listing.
- [ ] Sign with a new Meloqis release key and store it outside the repository.
- [ ] Configure only Axenora-owned OAuth clients, API keys, backends, domains, and contact addresses.
- [ ] Keep every OAuth client secret on a backend; never in the APK.
- [ ] Remove or rights-clear streaming, downloading, lyrics, artwork, canvas, recognition, and lossless catalog sources.
- [ ] Do not describe the app as “ad-free YouTube” or promise downloading from services whose terms prohibit it.
- [ ] Publish an accurate privacy policy and deletion/contact procedure.
- [ ] Complete Android developer verification and package-name registration where required.
- [ ] Run release build, tests, lint, dependency/security scan, malware scan, and device testing.
- [ ] Test Android 13 on the Samsung Galaxy S21 Ultra, including overlay permission denial and revocation.
- [ ] Verify app links for `meloqis.axenoraai.in` using Digital Asset Links.
- [ ] Decide separately between Google Play distribution and website sideloading; do not ship self-update install permission in a Play build.

## Meloqis Now Capsule acceptance checks

- [ ] Off by default on a clean install.
- [ ] Explains why “Display over other apps” is needed before opening system settings.
- [ ] Immediately disappears when disabled, permission is revoked, or playback has no current item.
- [ ] Previous, play/pause, next, expand/collapse, and long-press-to-open work.
- [ ] Does not obscure critical system controls or lock-screen security UI.
- [ ] Does not use notification access or AccessibilityService.
- [ ] Battery use remains acceptable during an hour of playback.

## Global operation

- [ ] Identify the legal operator and business address.
- [ ] Define supported countries based on catalog and privacy rights.
- [ ] Establish copyright/takedown and abuse contacts.
- [ ] Establish data retention, access, deletion, and incident-response procedures.
- [ ] Review consumer, privacy, tax, accessibility, music-rights, and platform obligations with qualified counsel.
