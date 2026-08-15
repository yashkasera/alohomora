# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest  | Yes       |
| Older   | No        |

Only the latest release receives security fixes. Upgrade to stay covered.

## Reporting a Vulnerability

**Do not open a public issue for security vulnerabilities.**

Email **yashkasera@icloud.com** with:

- Description of the vulnerability
- Steps to reproduce
- Impact assessment (if known)
- Suggested fix (if any)

You should receive an acknowledgement within 48 hours. Fixes for confirmed vulnerabilities will be released as soon as practical, with credit given unless you prefer to remain anonymous.

## Scope

Alohomora is a **debug-only** library. It is not designed to run in production builds (`alohomora-noop` replaces it via `releaseImplementation`). That said, the library handles network traffic, database contents, and error stack traces from the host app, so vulnerabilities in data handling, the TCP protocol, or the desktop companion are in scope.

Out of scope:
- The host app's own security posture
- Vulnerabilities that require physical access to an unlocked device with USB debugging enabled (this is the normal operating mode)
