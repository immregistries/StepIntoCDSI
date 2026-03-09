# FHIR Endpoint and CORS Testing

## Purpose

This document provides a quick reference for interacting with the Step Into CDSi FHIR servlet endpoint and verifying CORS behavior for browser-based clients.

---

## FHIR Base Endpoint

- Base endpoint: `https://yourserver/fhir`
- Example custom operation endpoint: `https://yourserver/fhir/$immdb-forecast`

Replace `https://yourserver` with your environment host.

---

## Operation Stub: `$immdb-forecast`

The following section is intentionally a stub and should be completed with project-specific details.

- Operation intent: `TODO`
- Expected input payload/profile: `TODO`
- Expected response structure: `TODO`
- Authentication/authorization requirements: `TODO`
- Known constraints and edge cases: `TODO`

---

## CORS Support

`FhirServlet` registers HAPI FHIR `CorsInterceptor` during server initialization.

Configured behavior:

- Allowed origins: `*`
- Allowed headers: `*`
- Allowed methods: `GET`, `POST`, `OPTIONS`

This enables browser preflight checks (`OPTIONS`) to succeed before cross-origin `POST` calls to operations such as `$immdb-forecast`.

---

## Verify CORS with `curl`

Use this preflight request to validate CORS headers from the FHIR endpoint:

```bash
curl -i -X OPTIONS https://yourserver/fhir/$immdb-forecast \
  -H "Origin: https://example.com" \
  -H "Access-Control-Request-Method: POST"
```

PowerShell-safe variant (prevents `$immdb` variable expansion):

```powershell
curl -i -X OPTIONS 'https://yourserver/fhir/$immdb-forecast' `
  -H "Origin: https://example.com" `
  -H "Access-Control-Request-Method: POST"
```

Expected result:

- HTTP status is typically `200` or `204`
- Response includes CORS headers such as:
  - `Access-Control-Allow-Origin`
  - `Access-Control-Allow-Methods`
  - `Access-Control-Allow-Headers`

---

## Troubleshooting

If preflight fails:

- Confirm the request reaches the same deployed application instance as your browser traffic.
- Confirm reverse proxies/load balancers are not stripping `OPTIONS` or CORS headers.
- Confirm the URL matches the deployed servlet mapping for `/fhir/*`.
- Re-test directly against the app host (bypassing proxy) to isolate infrastructure behavior.
