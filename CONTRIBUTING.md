# Contributing to Chrono Study

Thanks for wanting to improve Chrono Study! This guide keeps contributions smooth.

## Ground rules

- Be respectful — see [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).
- Discuss large changes in an issue first.
- Keep study-time logic **correct**: never count breaks/pauses, never treat planned duration as
  actual, never create duplicate session records.
- Do not commit secrets, keystores or generated build output.

## Development setup

1. Install JDK 17 and Android SDK Platform 35 + Build-Tools 35.0.0.
2. Fork and clone the repository.
3. Build and test before opening a PR.

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Branching & commits

- Branch from `main` using a descriptive name, e.g. `feat/landscape-clock`, `fix/timer-resume`.
- Use [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `docs:`,
  `refactor:`, `test:`, `chore:`.
- Keep pull requests focused and small.

## Coding style

- Kotlin official code style; Compose-first UI; no business logic in composables.
- Prefer immutable state and unidirectional data flow.
- Follow the version catalog in `gradle/libs.versions.toml`; avoid adding dependencies that do
  not materially help.
- Add unit tests for domain logic, especially timer transitions and study-time maths.

## Pull request checklist

- [ ] Builds locally (`assembleDebug`, `assembleRelease`).
- [ ] Tests pass (`testDebugUnitTest`).
- [ ] Lint passes (`lintDebug`).
- [ ] New behaviour is covered by tests where practical.
- [ ] Documentation updated when needed.

## Reporting bugs / requesting features

Use the issue templates. Include device, Android version, app version and clear reproduction
steps.

## License

By contributing you agree that your contributions are licensed under the [MIT License](LICENSE).
