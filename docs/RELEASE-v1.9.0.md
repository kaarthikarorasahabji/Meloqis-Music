Meloqis Music 1.9.0

- A more useful Home: direct search and collection shortcuts, a dedicated Edge lighting studio card, clearer turntable labels, and navigation that follows shortcuts correctly.
- Improved player text contrast over bright artwork, with larger, softer artwork corners.
- Studio notification scheduling now supports a delivery date and any time in India time. Automated checks run approximately every five minutes; platform queues, connectivity and device power saving can delay delivery.
- Required-update support for installations running 1.9.0 or later. A confirmed newer minimum version opens an app-wide update screen, including from secondary screens. Previously confirmed requirements work offline; devices without a confirmed policy remain usable offline. Android still asks the user to install the downloaded update.
- Edge lighting remains separate from the audio equalizer. No festival section has been added to Settings.

Distribution: choose the APK for your device, or use the universal APK. Existing APK installations retain the original release signing certificate. Play Store submission uses a separate upload-signed AAB.

Release policy: `update-policy.json` is generated alongside every release. `minimumVersion` applies to direct APK installations. `minimumPlayVersion` must be advanced only after Google Play makes that version available; it defaults to 0.0.0 while the first Play release is pending. This avoids sending Play users to an unavailable update. Owner rollback can lower or disable the policy.

This update cannot change the update behavior of older installed builds until their users install 1.9.0. Notification delivery requires listener opt-in, notification permission and a working Firebase registration.
