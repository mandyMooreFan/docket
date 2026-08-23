# Security policy

## Reporting a vulnerability

**Please do not open a public issue for a security problem.**

Report it privately through GitHub's private vulnerability reporting:
[**Report a vulnerability**](../../security/advisories/new) (also under the repo's
**Security** tab). It is enabled for this repository and goes only to the maintainer.

You'll get an acknowledgement, and the report stays private until a fix is out. Docket is
maintained by one person (this is stated honestly in the product too — `SPEC.md` §10.1), so
"promptly" means days, not hours.

## Scope

Docket is pre-launch: there is no hosted instance yet. Until one exists, reports about the
code on `main` are welcome — especially anything touching the areas the spec marks
load-bearing: the magic-link login flow, the visibility Dial and its floors, the under-18
protections (§9), and the upload pipeline.

## Supported versions

There are no releases yet; `main` is the only supported line.
