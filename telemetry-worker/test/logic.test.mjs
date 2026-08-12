import test from "node:test";
import assert from "node:assert/strict";
import {
  clampDays,
  cleanText,
  generateTotp,
  ratio,
  validateEvent,
  validateFeedback,
  verifyTotp,
} from "../src/logic.js";

test("dashboard range is constrained", () => {
  assert.equal(clampDays("2"), 7);
  assert.equal(clampDays("30"), 30);
  assert.equal(clampDays("500"), 90);
  assert.equal(clampDays("bad"), 30);
});

test("feedback validation keeps only bounded product feedback", () => {
  const feedback = validateFeedback({
    submissionId: "123e4567-e89b-42d3-a456-426614174000",
    rating: 5,
    category: "Design",
    message: "  The glass motion feels excellent.\nPlease add a calmer mode.  ",
    appVersion: "0.1.13",
    androidVersion: "13",
    sdkInt: 33,
    email: "must-not-be-retained@example.com",
  });

  assert.equal(feedback.rating, 5);
  assert.equal(feedback.category, "Design");
  assert.equal(feedback.message, "The glass motion feels excellent.\nPlease add a calmer mode.");
  assert.equal("email" in feedback, false);
});

test("invalid feedback is rejected", () => {
  assert.equal(validateFeedback({ rating: 6 }), null);
  assert.equal(
    validateFeedback({
      submissionId: "123e4567-e89b-42d3-a456-426614174000",
      rating: 4,
      category: "Passwords",
      message: "hello",
      appVersion: "0.1.13",
      androidVersion: "13",
      sdkInt: 33,
    }),
    null,
  );
});

test("crash and update feedback categories are accepted", () => {
  for (const category of ["Crash or bug", "Update"]) {
    assert.notEqual(
      validateFeedback({
        submissionId: "123e4567-e89b-42d3-a456-426614174000",
        rating: 3,
        category,
        message: "A concise reproducible report",
        appVersion: "0.1.13",
        androidVersion: "13",
        sdkInt: 33,
      }),
      null,
    );
  }
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
