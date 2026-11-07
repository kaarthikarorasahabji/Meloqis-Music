# Notification delivery operations

The Vercel notification panel remains at https://admin.axenoraai.in.

The primary automatic timer now runs in the existing active EmpMetria Supabase project, ksdbbcbkyypoxagjivxg. The job meloqis-notification-dispatch runs every five minutes (UTC). It posts to the existing Vercel dispatcher using a restricted credential held in Vault. HTTP status and safe result counts are retained for seven days in the private meloqis_scheduler.dispatch_checks table. Client roles cannot access the schema, records or secret. Campaign data remains in private Vercel Blob. The native 04:00 India-time task and daily Vercel backup remain available; atomic claims prevent duplicate sends.

GitHub scheduled events failed to run during the initial rollout. The GitHub workflow is now an owner-triggered manual backup only. Do not re-enable a scheduled GitHub checker in parallel: redundant empty checks consume the Blob read allowance.

A selected time is handled at the next five-minute tick. Android connectivity, notification permissions and power management can delay receipt. Owner Check due wishes now dispatches only due, unexpired campaigns and cannot send future wishes early.

Verification on 7 November 2026: the two user-created due wishes were accepted by Firebase and the user confirmed receipt on their phone. An automatic backend tick succeeded at 17:05 UTC (22:35 IST). After the synchronous HTTP/private-log configuration was installed, the next automatic tick at 17:10 UTC (22:40 IST) returned HTTP 200 with accepted=0, failed=0: prior wishes were not resent, and future festival wishes were left scheduled. No APK change is required for this infrastructure correction.

Recovery SQL and maintenance details are in the separate local admin source at meloqis-admin/vercel/ops/scheduler-setup.sql and meloqis-admin/vercel/README.md. Credentials are intentionally excluded from source and release packages.
