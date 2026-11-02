# Mechanisms

Physical facts the code depends on. One entry per mechanism.

## Intake

- One motor; forward pulls balls toward the indexing gate (CLAUDE.md §4.3).
- Actuator owner: `mechanisms/Intake` owns the intake motor.
- Current behavior: on/off only, at `Intake.power` (Panels-editable, default 1.0, clamped to -1.0..1.0).
  No reverse, no inventory or indexer interlock yet.
- Unmeasured: which motor direction pulls balls in.
