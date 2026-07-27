export const ALLOWED_EVENTS = new Set([
  "first_open",
  "app_active",
  "update_download_started",
  "update_download_completed",
  "update_download_failed",
  "update_verification_failed",
  "update_ready_to_install",
  "update_install_succeeded",
  "crash",
  "playback_failure",
]);

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const VERSION_PATTERN = /^[0-9A-Za-z][0-9A-Za-z.+_-]{0,31}$/;

export function clampDays(rawValue) {
  const value = Number.parseInt(String(rawValue ?? "30"), 10);
  if (!Number.isFinite(value)) return 30;
  return Math.min(90, Math.max(7, value));
}

export function cleanText(value, maxLength = 80) {
  if (typeof value !== "string") return null;
  const cleaned = value
    .replace(/[\u0000-\u001f\u007f]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  return cleaned ? cleaned.slice(0, maxLength) : null;
}

export function validateEvent(raw) {
  if (!raw || typeof raw !== "object") return null;
  if (!UUID_PATTERN.test(String(raw.eventId ?? ""))) return null;
  if (!ALLOWED_EVENTS.has(raw.name)) return null;
  if (!VERSION_PATTERN.test(String(raw.appVersion ?? ""))) return null;

  const versionCode = Number.parseInt(raw.versionCode, 10);
  const sdkInt = Number.parseInt(raw.sdkInt, 10);
  if (!Number.isInteger(versionCode) || versionCode < 1 || versionCode > 10_000_000) return null;
  if (!Number.isInteger(sdkInt) || sdkInt < 26 || sdkInt > 100) return null;

  return {
    eventId: raw.eventId.toLowerCase(),
    name: raw.name,
    appVersion: raw.appVersion,
    versionCode,
    androidVersion: cleanText(raw.androidVersion, 24) ?? "unknown",
    sdkInt,
    detailCode: cleanText(raw.details?.code),
    fromVersion: cleanText(raw.details?.fromVersion, 32),
    toVersion: cleanText(raw.details?.toVersion, 32),
    installKind: ["fresh", "upgrade", "unknown"].includes(raw.details?.installKind)
      ? raw.details.installKind
      : "unknown",
  };
}

export function ratio(numerator, denominator) {
  if (!denominator) return 0;
  return Math.round((Number(numerator) / Number(denominator)) * 1000) / 10;
}

export function decodeBase32(value) {
  const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  const cleaned = String(value).toUpperCase().replace(/=+$/, "").replace(/\s+/g, "");
  let bits = 0;
  let buffer = 0;
  const bytes = [];
  for (const character of cleaned) {
    const index = alphabet.indexOf(character);
    if (index < 0) throw new Error("Invalid base32 secret");
    buffer = (buffer << 5) | index;
    bits += 5;
    if (bits >= 8) {
      bytes.push((buffer >>> (bits - 8)) & 0xff);
      bits -= 8;
    }
  }
  return new Uint8Array(bytes);
}

export async function generateTotp(secret, timestamp = Date.now()) {
  const counter = Math.floor(timestamp / 30_000);
  const counterBytes = new Uint8Array(8);
  let remaining = counter;
  for (let index = 7; index >= 0; index -= 1) {
    counterBytes[index] = remaining & 0xff;
    remaining = Math.floor(remaining / 256);
  }
  const key = await crypto.subtle.importKey(
    "raw",
    decodeBase32(secret),
    { name: "HMAC", hash: "SHA-1" },
    false,
    ["sign"],
  );
  const digest = new Uint8Array(await crypto.subtle.sign("HMAC", key, counterBytes));
  const offset = digest[digest.length - 1] & 0x0f;
  const value =
    ((digest[offset] & 0x7f) << 24) |
    ((digest[offset + 1] & 0xff) << 16) |
    ((digest[offset + 2] & 0xff) << 8) |
    (digest[offset + 3] & 0xff);
  return String(value % 1_000_000).padStart(6, "0");
}

export async function verifyTotp(secret, code, timestamp = Date.now()) {
  if (!/^\d{6}$/.test(String(code))) return false;
  const candidates = await Promise.all(
    [-30_000, 0, 30_000].map((offset) => generateTotp(secret, timestamp + offset)),
  );
  return candidates.some((candidate) => candidate === code);
}
