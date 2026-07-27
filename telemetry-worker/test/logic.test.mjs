import test from "node:test";
import assert from "node:assert/strict";
import {
  clampDays,
  cleanText,
  generateTotp,
  ratio,
  validateEvent,
  verifyTotp,
} from "../src/logic.js";

test("dashboard range is constrained", () => {
  assert.equal(clampDays("2"), 7);
  assert.equal(clampDays("30"), 30);
  assert.equal(clampDays("500"), 90);
  assert.equal(clampDays("bad"), 30);
});

test("event validation strips unexpected details", () => {
  const event = validateEvent({
    eventId: "123e4567-e89b-42d3-a456-426614174000",
    name: "playback_failure",
    appVersion: "0.1.8",
    versionCode: 9,
    androidVersion: "13",
    sdkInt: 33,
    details: {
      code: "  HTTP 403\n",
      songTitle: "must not be retained",
    },
  });

  assert.equal(event.detailCode, "HTTP 403");
  assert.equal("songTitle" in event, false);
});

test("invalid or unknown events are rejected", () => {
  assert.equal(validateEvent({}), null);
  assert.equal(
    validateEvent({
      eventId: "123e4567-e89b-42d3-a456-426614174000",
      name: "track_title",
      appVersion: "0.1.8",
      versionCode: 9,
      androidVersion: "13",
      sdkInt: 33,
    }),
    null,
  );
});

test("ratio handles empty denominators", () => {
  assert.equal(ratio(3, 0), 0);
  assert.equal(ratio(3, 8), 37.5);
});

test("TOTP follows the RFC 6238 SHA-1 vector", async () => {
  const secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
  assert.equal(await generateTotp(secret, 59_000), "287082");
  assert.equal(await verifyTotp(secret, "287082", 59_000), true);
  assert.equal(await verifyTotp(secret, "000000", 59_000), false);
});
