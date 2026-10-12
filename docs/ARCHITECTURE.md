# Architecture

Chrono Study uses a layered, unidirectional architecture with a strict separation of concerns.

```
┌──────────────────────────────────────────────────────────────────────┐
│ Presentation   Compose screens · reusable components · ViewModels     │
├──────────────────────────────────────────────────────────────────────┤
│ Domain         Timer state machine · elapsed-time maths · statistics  │
│                validation · pure, testable, no Android dependencies    │
├──────────────────────────────────────────────────────────────────────┤
│ Data           Room entities/DAOs · repositories · DataStore settings │
├──────────────────────────────────────────────────────────────────────┤
│ Integration    Notifications · vibration · exact alarms · foreground  │
│                service · display/lifecycle behaviour                  │
└──────────────────────────────────────────────────────────────────────┘
```

## Layers

### Presentation (`ui/`)
- Compose screens (`ui/clock`, `ui/home`, `ui/timer`, `ui/library`, `ui/planner`,
  `ui/history`, `ui/analytics`, `ui/settings`) and reusable components (`ui/components`).
- One `ViewModel` per screen exposing a single immutable UI state via `StateFlow`.
- No business logic in composables; navigation lives in `ui/navigation`.

### Domain (`domain/`)
- `TimerEngine` / `TimerState`: explicit state machine — `Idle → Running → Paused →
  Completed / Cancelled` — with legal transitions and guarded invalid transitions.
- Elapsed time is computed from persisted timestamps and a monotonic clock, never by
  decrementing a counter, so it survives rotation, backgrounding, process death and reboot.
- `StatsCalculator` derives totals, streaks and chart series from saved sessions.

### Data (`data/`)
- Room entities: `Subject`, `Chapter`, `StudySession`, `PlannerTask`, `DailyGoal`.
- DAOs expose `Flow`s; repositories combine and validate data.
- `SettingsRepository` wraps DataStore for themes, timer defaults and display options.
- Backup/restore uses versioned, validated kotlinx.serialization models.

### Integration (`notification/`, `alarm/`, `service/`, `alert/`)
- Notification channels for focus, break and reminders.
- Exact alarms signal expected completion; a fallback is used when exact-alarm access is denied.
- A user-initiated foreground service surfaces the ongoing timer; it is started only during an
  active session and stopped when the session ends.
- Completion handlers are idempotent: duplicate callbacks cannot create duplicate records or
  alerts.

## Threading

- Database and I/O off the main thread via coroutines and `Dispatchers.IO`.
- UI collects state with lifecycle-aware `collectAsStateWithLifecycle`.
- No busy loops or polling; countdown UI derives from state and recomputes on resume.

## Reliability principles

1. Accuracy first — only eligible focus time is counted.
2. Idempotency — completion and alarm paths are safe to run more than once.
3. Recoverability — persisted state reconstructs an interrupted session.
4. Privacy — local-first, no network required, no telemetry.
