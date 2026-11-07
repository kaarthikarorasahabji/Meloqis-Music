Release verification

- Android debug compilation and 16 host tests passed, including numeric version ordering, invalid policy rejection and India-time scheduling across midnight.
- Nine admin tests passed: sessions, origin protection, date/time validation, invalid calendar rejection, optimistic concurrency and one-claim delivery.
- Production admin date/time draft creation, editing, read-back and cancellation passed. The disposable draft was removed; the three original scheduled wishes remain unchanged at 04:00 IST.
- The GitHub delivery-check workflow completed successfully against production with no due public campaigns. Scheduled checks are best effort; exact-minute delivery is not promised.
- A real, single-device Firebase message sent through the production owner endpoint was received by the owned Android emulator. Android posted its notification and Meloqis queued its animated wish. Public topic users did not receive the test.
- An Android instrumentation check confirmed that a cached higher required version redirects the separate Edge lighting activity to the required-update activity.
- Previous edge renderer checks cover 42 mode/position combinations, four clocks, quiet overlay and notch fitting. Full physical-device visual QA, battery/OEM behavior and Google Play submission remain outstanding.

The custom instrumentation fixtures require explicit Gradle properties and are not included in release APKs. Device registration tokens are kept in private fixture storage and removed after testing.
