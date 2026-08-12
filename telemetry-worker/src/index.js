import { DASHBOARD_HTML } from "./dashboard.js";
import { clampDays, ratio, validateEvent, validateFeedback } from "./logic.js";

const encoder = new TextEncoder();
const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const SESSION_COOKIE = "meloqis_admin";
const MAX_EVENTS_PER_REQUEST = 32;
const MAX_DAILY_EVENTS_PER_INSTALL = 500;
const REGISTER_LIMIT_PER_DAY = 20;
const EVENT_LIMIT_PER_HOUR = 240;
const DOWNLOAD_LIMIT_PER_HOUR = 300;
const FEEDBACK_LIMIT_PER_DAY = 5;

export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);

      if (request.method === "GET" && url.pathname === "/") {
        return html(DASHBOARD_HTML);
      }
      if (request.method === "GET" && url.pathname === "/health") {
        return json({ ok: true, service: "meloqis-insights" });
      }
      if (request.method === "POST" && url.pathname === "/api/events") {
        return ingestEvents(request, env);
      }
      if (request.method === "POST" && url.pathname === "/api/register") {
        return registerInstallation(request, env);
      }
      if (request.method === "POST" && url.pathname === "/api/feedback") {
        return submitFeedback(request, env);
      }
      if (request.method === "POST" && url.pathname === "/api/login") {
        return login(request, env);
      }
      if (request.method === "POST" && url.pathname === "/api/logout") {
        return logout();
      }
      if (request.method === "GET" && url.pathname === "/api/dashboard") {
        if (!(await isAuthenticated(request, env))) return unauthorized();
        return dashboard(env, clampDays(url.searchParams.get("days")));
      }
      if (request.method === "GET" && url.pathname === "/api/admin-artifact") {
        if (!(await isAuthenticated(request, env))) return unauthorized();
        return adminArtifact(env);
      }
      if (request.method === "GET" && url.pathname === "/download/latest") {
        return trackedDownload(request, env);
      }
      return json({ error: "Not found" }, 404);
    } catch (error) {
      console.error("Unhandled request failure", error);
      return json({ error: "Service temporarily unavailable" }, 500);
    }
  },
  async scheduled(_controller, env) {
    await env.DB.batch([
      env.DB.prepare("DELETE FROM events WHERE created_at < datetime('now', '-90 days')"),
      env.DB.prepare("DELETE FROM download_events WHERE created_at < datetime('now', '-400 days')"),
      env.DB.prepare("DELETE FROM installations WHERE last_seen < datetime('now', '-400 days')"),
      env.DB.prepare(
        "DELETE FROM login_attempts WHERE first_failure_at < datetime('now', '-1 day')",
      ),
      env.DB.prepare("DELETE FROM rate_limits WHERE updated_at < datetime('now', '-2 days')"),
    ]);
  },
};

async function registerInstallation(request, env) {
  if (!request.headers.get("content-type")?.toLowerCase().includes("application/json")) {
    return json({ error: "JSON required" }, 415);
  }
  if (!(await consumeRateLimit(request, env, "register", 86_400_000, REGISTER_LIMIT_PER_DAY))) {
    return json({ error: "Registration limit reached" }, 429);
  }
  const body = await request.json().catch(() => null);
  const installationId = String(body?.installationId ?? "").toLowerCase();
  if (!UUID_PATTERN.test(installationId)) return json({ error: "Invalid installation" }, 400);
  return json({ installToken: await installationToken(installationId, env.INSTALL_TOKEN_SECRET) });
}

async function ingestEvents(request, env) {
  const contentLength = Number.parseInt(request.headers.get("content-length") ?? "0", 10);
  if (contentLength > 64 * 1024) return json({ error: "Payload too large" }, 413);
  if (!request.headers.get("content-type")?.toLowerCase().includes("application/json")) {
    return json({ error: "JSON required" }, 415);
  }

  const body = await request.json().catch(() => null);
  const installationId = String(body?.installationId ?? "").toLowerCase();
  if (!UUID_PATTERN.test(installationId)) return json({ error: "Invalid installation" }, 400);
  const suppliedToken = String(body?.installToken ?? "");
  const expectedToken = await installationToken(installationId, env.INSTALL_TOKEN_SECRET);
  if (!constantTimeEqual(suppliedToken, expectedToken)) return unauthorized();
  if (!(await consumeRateLimit(request, env, "events", 3_600_000, EVENT_LIMIT_PER_HOUR))) {
    return json({ error: "Upload limit reached" }, 429);
  }
  if (!Array.isArray(body?.events) || body.events.length < 1 || body.events.length > MAX_EVENTS_PER_REQUEST) {
    return json({ error: "Invalid event batch" }, 400);
  }

  const events = body.events.map(validateEvent);
  if (events.some((event) => event === null)) return json({ error: "Invalid event" }, 400);

  const installHash = await sha256Hex(installationId);
  const since = new Date(Date.now() - 86_400_000).toISOString();
  const recentCount = await env.DB.prepare(
    "SELECT COUNT(*) AS count FROM events WHERE install_hash = ? AND created_at >= ?",
  )
    .bind(installHash, since)
    .first("count");
  if (Number(recentCount ?? 0) + events.length > MAX_DAILY_EVENTS_PER_INSTALL) {
    return json({ error: "Daily event limit reached" }, 429);
  }

  const now = new Date().toISOString();
  const latest = events.at(-1);
  const firstOpen = events.find((event) => event.name === "first_open");
  const statements = [
    env.DB.prepare(
      `INSERT INTO installations (
          install_hash, first_seen, last_seen, first_open_at, first_open_kind,
          app_version, version_code, android_version, sdk_int
       ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(install_hash) DO UPDATE SET
          last_seen = excluded.last_seen,
          first_open_at = COALESCE(installations.first_open_at, excluded.first_open_at),
          first_open_kind = CASE
            WHEN installations.first_open_at IS NULL THEN excluded.first_open_kind
            ELSE installations.first_open_kind
          END,
          app_version = excluded.app_version,
          version_code = excluded.version_code,
          android_version = excluded.android_version,
          sdk_int = excluded.sdk_int`,
    ).bind(
      installHash,
      now,
      now,
      firstOpen ? now : null,
      firstOpen?.installKind ?? null,
      latest.appVersion,
      latest.versionCode,
      latest.androidVersion,
      latest.sdkInt,
    ),
    ...events.map((event) =>
      env.DB.prepare(
        `INSERT OR IGNORE INTO events (
            event_id, install_hash, event_name, created_at, app_version,
            version_code, android_version, sdk_int, detail_code, from_version, to_version
         ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      ).bind(
        event.eventId,
        installHash,
        event.name,
        now,
        event.appVersion,
        event.versionCode,
        event.androidVersion,
        event.sdkInt,
        event.detailCode,
        event.fromVersion,
        event.toVersion,
      ),
    ),
  ];

  await env.DB.batch(statements);
  return json({ accepted: events.length }, 202);
}

async function submitFeedback(request, env) {
  const contentLength = Number.parseInt(request.headers.get("content-length") ?? "0", 10);
  if (contentLength > 12 * 1024) return json({ error: "Payload too large" }, 413);
  if (!request.headers.get("content-type")?.toLowerCase().includes("application/json")) {
    return json({ error: "JSON required" }, 415);
  }
  if (!(await consumeRateLimit(request, env, "feedback", 86_400_000, FEEDBACK_LIMIT_PER_DAY))) {
    return json({ error: "Feedback limit reached" }, 429);
  }

  const feedback = validateFeedback(await request.json().catch(() => null));
  if (!feedback) return json({ error: "Invalid feedback" }, 400);
  if (!env.RESEND_API_KEY || !env.FEEDBACK_FROM || !env.FEEDBACK_TO) {
    console.error("Feedback email configuration is incomplete");
    return json({ error: "Feedback service unavailable" }, 503);
  }

  const emailResponse = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      authorization: `Bearer ${env.RESEND_API_KEY}`,
      "content-type": "application/json",
      "idempotency-key": `meloqis-feedback-${feedback.submissionId}`,
    },
    body: JSON.stringify({
      from: env.FEEDBACK_FROM,
      to: [env.FEEDBACK_TO],
      subject: `Meloqis feedback: ${feedback.rating}/5 · ${feedback.category}`,
      text: [
        "New feedback from Meloqis Music",
        "",
        `Rating: ${feedback.rating}/5`,
        `Category: ${feedback.category}`,
        `App version: ${feedback.appVersion}`,
        `Android: ${feedback.androidVersion} (SDK ${feedback.sdkInt})`,
        `Submission: ${feedback.submissionId}`,
        "",
        feedback.message,
      ].join("\n"),
    }),
  });

  if (!emailResponse.ok) {
    const errorCode = emailResponse.status;
    console.error("Resend feedback delivery failed", errorCode);
    return json({ error: "Feedback delivery failed" }, 502);
  }
  return json({ accepted: true }, 202);
}

async function login(request, env) {
  if (!request.headers.get("content-type")?.toLowerCase().includes("application/json")) {
    return json({ error: "JSON required" }, 415);
  }

  const fingerprint = await loginFingerprint(request, env);
  const attempt = await env.DB.prepare(
    "SELECT failures, first_failure_at, blocked_until FROM login_attempts WHERE fingerprint = ?",
  )
    .bind(fingerprint)
    .first();
  if (attempt?.blocked_until && Date.parse(attempt.blocked_until) > Date.now()) {
    return json({ error: "Try again later" }, 429);
  }

  const body = await request.json().catch(() => null);
  const passwordMatches =
    typeof body?.password === "string" &&
    body.password.length <= 256 &&
    (await verifyPassword(body.password, env.ADMIN_PASSWORD_HASH));

  if (!passwordMatches) {
    await recordFailedLogin(env, fingerprint, attempt);
    return unauthorized();
  }

  await env.DB.prepare("DELETE FROM login_attempts WHERE fingerprint = ?").bind(fingerprint).run();
  const token = await createSession(env.ADMIN_USERNAME, env.SESSION_SECRET);
  return new Response(null, {
    status: 204,
    headers: {
      "set-cookie": `${SESSION_COOKIE}=${token}; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=7200`,
      "cache-control": "no-store",
    },
  });
}

function logout() {
  return new Response(null, {
    status: 204,
    headers: {
      "set-cookie": `${SESSION_COOKIE}=; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=0`,
      "cache-control": "no-store",
    },
  });
}

async function dashboard(env, days) {
  const cutoff = new Date(Date.now() - days * 86_400_000).toISOString();
  const cutoff7 = new Date(Date.now() - 7 * 86_400_000).toISOString();
  const cutoff30 = new Date(Date.now() - 30 * 86_400_000).toISOString();

  const [
    totalFirstOpens,
    active7,
    active30,
    downloads,
    updateDownloads,
    updateSuccess,
    crashes,
    playbackFailures,
    activityResult,
    versionsResult,
    failuresResult,
  ] = await Promise.all([
    scalar(env, "SELECT COUNT(*) AS value FROM installations WHERE first_open_at IS NOT NULL"),
    scalar(env, "SELECT COUNT(*) AS value FROM installations WHERE last_seen >= ?", cutoff7),
    scalar(env, "SELECT COUNT(*) AS value FROM installations WHERE last_seen >= ?", cutoff30),
    scalar(env, "SELECT COUNT(*) AS value FROM download_events WHERE created_at >= ?", cutoff),
    scalar(
      env,
      "SELECT COUNT(*) AS value FROM events WHERE event_name = 'update_download_started' AND created_at >= ?",
      cutoff,
    ),
    scalar(
      env,
      "SELECT COUNT(*) AS value FROM events WHERE event_name = 'update_install_succeeded' AND created_at >= ?",
      cutoff,
    ),
    scalar(env, "SELECT COUNT(*) AS value FROM events WHERE event_name = 'crash' AND created_at >= ?", cutoff30),
    scalar(
      env,
      "SELECT COUNT(*) AS value FROM events WHERE event_name = 'playback_failure' AND created_at >= ?",
      cutoff30,
    ),
    env.DB.prepare(
      `SELECT substr(created_at, 1, 10) AS day, COUNT(DISTINCT install_hash) AS active
       FROM events
       WHERE created_at >= ?
       GROUP BY substr(created_at, 1, 10)
       ORDER BY day`,
    )
      .bind(cutoff)
      .all(),
    env.DB.prepare(
      `SELECT app_version AS version, COUNT(*) AS devices
       FROM installations
       WHERE last_seen >= ?
       GROUP BY app_version
       ORDER BY version_code DESC, devices DESC
       LIMIT 12`,
    )
      .bind(cutoff30)
      .all(),
    env.DB.prepare(
      `SELECT created_at AS createdAt, event_name AS name, app_version AS appVersion,
              detail_code AS code
       FROM events
       WHERE created_at >= ? AND event_name IN ('crash', 'playback_failure', 'update_download_failed', 'update_verification_failed')
       ORDER BY created_at DESC
       LIMIT 40`,
    )
      .bind(cutoff)
      .all(),
  ]);

  const versions = versionsResult.results ?? [];
  const latestVersion = versions[0]?.version ?? null;
  const latestDevices = versions.find((row) => row.version === latestVersion)?.devices ?? 0;

  return json({
    generatedAt: new Date().toISOString(),
    days,
    kpis: {
      totalFirstOpens,
      active7,
      active30,
      downloads,
      updateDownloads,
      updateSuccess,
      latestVersion,
      latestAdoption: ratio(latestDevices, active30),
    },
    reliability: {
      crashes,
      playbackFailures,
      crashesPer100: ratio(crashes, active30),
      playbackFailuresPer100: ratio(playbackFailures, active30),
    },
    activity: fillDailySeries(activityResult.results ?? [], days),
    versions,
    recentFailures: failuresResult.results ?? [],
  });
}

async function trackedDownload(request, env) {
  if (!(await consumeRateLimit(request, env, "download", 3_600_000, DOWNLOAD_LIMIT_PER_HOUR))) {
    return json({ error: "Download limit reached" }, 429);
  }
  const response = await fetch(env.APK_MANIFEST_URL, {
    headers: {
      accept: "application/json",
      "cache-control": "no-cache",
      "user-agent": "Meloqis-Insights/0.1.9",
    },
    cf: { cacheTtl: 0 },
  });
  if (!response.ok) return json({ error: "Release is temporarily unavailable" }, 503);
  const manifest = await response.json();
  const downloadUrl = new URL(manifest.downloadUrl);
  if (
    downloadUrl.protocol !== "https:" ||
    downloadUrl.hostname !== "pub-7cad9af12a364d3f928f96a083db320f.r2.dev"
  ) {
    return json({ error: "Release URL was rejected" }, 502);
  }

  const version = String(manifest.version ?? "unknown").slice(0, 32);
  const source = new URL(request.url).searchParams.get("source") === "app" ? "app" : "website";
  await env.DB.prepare(
    "INSERT INTO download_events (created_at, app_version, source) VALUES (?, ?, ?)",
  )
    .bind(new Date().toISOString(), version, source)
    .run();

  return Response.redirect(downloadUrl.toString(), 302);
}

async function adminArtifact(env) {
  const object = await env.ADMIN_ARTIFACTS.get("meloqis-insights-admin-0.1.9.apk");
  if (!object) return json({ error: "Admin artifact is not available yet" }, 404);
  const headers = new Headers();
  object.writeHttpMetadata(headers);
  headers.set("content-type", "application/vnd.android.package-archive");
  headers.set(
    "content-disposition",
    'attachment; filename="meloqis-insights-admin-0.1.9.apk"',
  );
  headers.set("cache-control", "private, no-store");
  headers.set("x-content-type-options", "nosniff");
  if (object.size) headers.set("content-length", String(object.size));
  return new Response(object.body, { headers });
}

async function scalar(env, sql, ...bindings) {
  const statement = env.DB.prepare(sql);
  const value = bindings.length
    ? await statement.bind(...bindings).first("value")
    : await statement.first("value");
  return Number(value ?? 0);
}

function fillDailySeries(rows, days) {
  const values = new Map(rows.map((row) => [row.day, Number(row.active)]));
  const series = [];
  for (let offset = days - 1; offset >= 0; offset -= 1) {
    const date = new Date(Date.now() - offset * 86_400_000).toISOString().slice(0, 10);
    series.push({ day: date, active: values.get(date) ?? 0 });
  }
  return series;
}

async function recordFailedLogin(env, fingerprint, previous) {
  const now = new Date();
  const firstFailure = previous?.first_failure_at ? new Date(previous.first_failure_at) : now;
  const windowExpired = now.getTime() - firstFailure.getTime() > 15 * 60_000;
  const failures = windowExpired ? 1 : Number(previous?.failures ?? 0) + 1;
  const blockedUntil = failures >= 8 ? new Date(now.getTime() + 15 * 60_000).toISOString() : null;

  await env.DB.prepare(
    `INSERT INTO login_attempts (fingerprint, failures, first_failure_at, blocked_until)
     VALUES (?, ?, ?, ?)
     ON CONFLICT(fingerprint) DO UPDATE SET
       failures = excluded.failures,
       first_failure_at = excluded.first_failure_at,
       blocked_until = excluded.blocked_until`,
  )
    .bind(fingerprint, failures, windowExpired ? now.toISOString() : firstFailure.toISOString(), blockedUntil)
    .run();
}

async function loginFingerprint(request, env) {
  const address = request.headers.get("cf-connecting-ip") ?? "unknown";
  return sha256Hex(`${address}:${env.SESSION_SECRET}`);
}

async function consumeRateLimit(request, env, scope, windowMs, limit) {
  const address = request.headers.get("cf-connecting-ip") ?? "unknown";
  const bucket = Math.floor(Date.now() / windowMs);
  const keyHash = await sha256Hex(
    `${scope}:${bucket}:${address}:${env.INSTALL_TOKEN_SECRET}`,
  );
  const updatedAt = new Date().toISOString();
  await env.DB.prepare(
    `INSERT INTO rate_limits (key_hash, request_count, updated_at)
     VALUES (?, 1, ?)
     ON CONFLICT(key_hash) DO UPDATE SET
       request_count = rate_limits.request_count + 1,
       updated_at = excluded.updated_at`,
  )
    .bind(keyHash, updatedAt)
    .run();
  const count = await env.DB.prepare(
    "SELECT request_count FROM rate_limits WHERE key_hash = ?",
  )
    .bind(keyHash)
    .first("request_count");
  return Number(count ?? limit + 1) <= limit;
}

async function installationToken(installationId, secret) {
  return sign(`install:${installationId}`, secret);
}

async function createSession(username, secret) {
  const payload = toBase64Url(
    encoder.encode(JSON.stringify({ username, expiresAt: Date.now() + 2 * 60 * 60_000 })),
  );
  return `${payload}.${await sign(payload, secret)}`;
}

async function isAuthenticated(request, env) {
  const cookie = request.headers.get("cookie") ?? "";
  const token = cookie
    .split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith(`${SESSION_COOKIE}=`))
    ?.slice(SESSION_COOKIE.length + 1);
  if (!token) return false;

  const [payload, signature, extra] = token.split(".");
  if (!payload || !signature || extra) return false;
  const expected = await sign(payload, env.SESSION_SECRET);
  if (!constantTimeEqual(signature, expected)) return false;

  const parsed = JSON.parse(new TextDecoder().decode(fromBase64Url(payload)));
  return (
    parsed.username === env.ADMIN_USERNAME &&
    Number.isFinite(parsed.expiresAt) &&
    parsed.expiresAt > Date.now()
  );
}

async function sign(value, secret) {
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  return toBase64Url(new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(value))));
}

async function verifyPassword(password, encodedHash) {
  if (typeof encodedHash !== "string" || !/^[0-9a-f]{64}$/.test(encodedHash)) return false;
  // The digest is stored as an encrypted Worker secret and is never shipped
  // in the dashboard, Android artifact, or repository.
  return constantTimeEqual(await sha256Hex(password), encodedHash);
}

async function sha256Hex(value) {
  const digest = new Uint8Array(await crypto.subtle.digest("SHA-256", encoder.encode(value)));
  return [...digest].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

function constantTimeEqual(first, second) {
  return constantTimeEqualBytes(encoder.encode(first), encoder.encode(second));
}

function constantTimeEqualBytes(first, second) {
  if (first.length !== second.length) return false;
  let difference = 0;
  for (let index = 0; index < first.length; index += 1) difference |= first[index] ^ second[index];
  return difference === 0;
}

function toBase64Url(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");
}

function fromBase64Url(value) {
  const normalized = value.replaceAll("-", "+").replaceAll("_", "/");
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
  const binary = atob(padded);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function unauthorized() {
  return json({ error: "Unauthorized" }, 401);
}

function html(body) {
  return new Response(body, {
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "no-store",
      "content-security-policy":
        "default-src 'self'; img-src 'self' data:; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'",
      "referrer-policy": "no-referrer",
      "x-content-type-options": "nosniff",
      "x-frame-options": "DENY",
    },
  });
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
    },
  });
}
