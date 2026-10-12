# Security Policy

## Supported versions

Chrono Study is under active development. Security fixes are applied to the latest release and
the `main` branch.

## Reporting a vulnerability

Please **do not** open a public issue for security problems. Instead, report privately using
GitHub's [private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
on the repository's **Security** tab, or contact the maintainer directly.

Include:

- A description of the issue and its impact
- Steps to reproduce
- Affected version / commit
- Any suggested fix

We aim to acknowledge reports within 72 hours.

## Scope & data handling

Chrono Study is local-first: it does not transmit study data anywhere. Relevant risks include
the handling of backup/export files and the validation of imported data. Reports about unsafe
deserialization, path handling or data injection on restore are especially welcome.
