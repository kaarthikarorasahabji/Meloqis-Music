import { readFile } from "node:fs/promises";
import { generateTotp } from "../src/logic.js";

const baseUrl =
  process.env.MELOQIS_INSIGHTS_BASE_URL ||
  "https://meloqis-insights.axenora-meloqis.workers.dev";
const credentialsPath = new URL("../../.artifacts/meloqis-insights-admin.txt", import.meta.url);
const credentials = await readFile(credentialsPath, "utf8");
const username = credentials.match(/^Administrator ID:\s*(.+)$/m)?.[1]?.trim();
const password = credentials.match(/^Password:\s*(.+)$/m)?.[1]?.trim();
const totpSecret = credentials.match(/^Authenticator secret:\s*(.+)$/m)?.[1]?.trim();
if (!username || !password || !totpSecret) throw new Error("Local admin credentials are incomplete");

const health = await fetch(`${baseUrl}/health`);
if (!health.ok) throw new Error(`Health failed: ${health.status}`);

const anonymousDashboard = await fetch(`${baseUrl}/api/dashboard`);
if (anonymousDashboard.status !== 401) {
  throw new Error(`Anonymous dashboard was not rejected: ${anonymousDashboard.status}`);
}

const installationId = "123e4567-e89b-42d3-a456-426614174000";
const registrationResponse = await fetch(`${baseUrl}/api/register`, {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ installationId }),
});
if (!registrationResponse.ok) {
  throw new Error(`Installation registration failed: ${registrationResponse.status}`);
}
const { installToken } = await registrationResponse.json();
if (!installToken) throw new Error("Installation registration did not return a token");

const eventResponse = await fetch(`${baseUrl}/api/events`, {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({
    installationId,
    installToken,
    events: [
      {
        eventId: "223e4567-e89b-42d3-a456-426614174000",
        name: "first_open",
        appVersion: "0.1.8",
        versionCode: 9,
        androidVersion: "13",
        sdkInt: 33,
        details: { installKind: "upgrade" },
      },
    ],
  }),
});
if (eventResponse.status !== 202) throw new Error(`Event ingest failed: ${eventResponse.status}`);

const loginResponse = await fetch(`${baseUrl}/api/login`, {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({
    username,
    password,
    otp: await generateTotp(totpSecret),
  }),
});
if (loginResponse.status !== 204) {
  throw new Error(`Login failed: ${loginResponse.status} ${await loginResponse.text()}`);
}
const cookie = loginResponse.headers.get("set-cookie")?.split(";", 1)[0];
if (!cookie) throw new Error("Login did not return a secure session");

const dashboardResponse = await fetch(`${baseUrl}/api/dashboard?days=30`, {
  headers: { cookie },
});
if (!dashboardResponse.ok) throw new Error(`Dashboard failed: ${dashboardResponse.status}`);
const dashboard = await dashboardResponse.json();
if (!dashboard.kpis || !Array.isArray(dashboard.activity)) {
  throw new Error("Dashboard response is malformed");
}

const artifactResponse = await fetch(`${baseUrl}/api/admin-artifact`, {
  headers: { cookie, range: "bytes=0-3" },
});
if (!artifactResponse.ok) throw new Error(`Private artifact failed: ${artifactResponse.status}`);
const artifact = new Uint8Array(await artifactResponse.arrayBuffer());
if (artifact[0] !== 0x50 || artifact[1] !== 0x4b) {
  throw new Error("Private admin artifact is not an APK/ZIP");
}

console.log(
  JSON.stringify({
    health: "ok",
    anonymousDashboard: "blocked",
    installationRegistration: "ok",
    eventIngest: "accepted",
    mfaLogin: "ok",
    dashboard: "ok",
    privateArtifact: "ok",
  }),
);
