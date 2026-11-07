# Mechanisms

Physical facts the code depends on. One entry per mechanism.

## Intake

- One motor; forward pulls balls toward the indexing gate (CLAUDE.md §4.3).
- Actuator owner: `mechanisms/Intake` owns the intake motor.
- Current behavior: on/off only, at `Intake.power` (Panels-editable, default 1.0, clamped to -1.0..1.0).
  No reverse, no inventory or indexer interlock yet.
- Unmeasured: which motor direction pulls balls in.

## Transfer

- One motor driving the belt-coupled rollers over the transfer ramp (CLAUDE.md §4.6).
- Actuator owner: `mechanisms/Transfer` owns the transfer motor.
- Current behavior: `run` command at `Transfer.power` (Panels-editable, default 1.0, clamped to -1.0..1.0), like Intake.
  FullTest toggles it with cross. No feeder interlock or fault handling yet.
- Unmeasured: which motor direction pushes balls toward the feeder.

## Turret

- One REV UltraPlanetary motor; motor encoder is the only feedback (relative, no absolute encoder) (CLAUDE.md §4.8).
- Actuator owner: `mechanisms/Turret` owns the turret motor.
- Reference: place the turret at the reference before powering the robot. The hub zeroes the encoder at power-up;
  the code never resets it, so the angle survives OpMode changes.
- Turret angles (radians in code, degrees in telemetry) are clockwise from the reference. Soft limits `MIN_RAD`..`MAX_RAD`
  = -60°..+60° (team, 2026-11-03); every setpoint is clamped.
- Commands: `fixedAngle` (default) holds `targetRad`. `lookAt(target, pose)` sets `targetRad` toward
  `target` (Pedro field inches; default: Panels `Turret.lookX/lookY`, read live) from the Pedro pose. `step(rad)` shifts it; dpad left/right step 10° in
  FullTest and TurretTest, which both start at 0°. FullTest's circle toggles `lookAt`. The `targetRad` setter
  (`chooseTurretAngle`) takes the equivalent angle (±360°) nearest the current target, unwinds the other way round
  when that one is past a limit, and parks at the nearer limit when no equivalent is reachable. Works for limits
  wider than 360°; unit-tested in `TurretAngleTest`. `targetRad` is not in Panels.
  Gains in Panels under `Turret.gains` (kP/kI/kD in `pidConstants`, kS in `ffCoefficients`), all 0.
- Geometry assumptions (unmeasured): turret axis at the Pinpoint origin, reference facing robot forward.
- Unmeasured: gearing (`RADIANS_PER_COUNT` = 2π / (28 × gearbox ratio × turret ratio); while 0 the encoder
  always reads 0°, so any kP > 0 with a non-zero target spins the motor without stopping), usable travel beyond ±60°, and which motor direction turns clockwise.
- Limitation: PID can overshoot past the soft limits; nothing stops the motor outside them yet.

## Shooter

- One flywheel motor with velocity PID, plus a hood servo (CLAUDE.md §4.9, §4.10 describe the final launcher).
- Actuator owner: `mechanisms/Shooter` owns the flywheel motor and the hood servo.
- Speed is measured with the motor's own encoder, so RPM is at the motor shaft, not the flywheel. The Through Bore
  encoder can't be used with NextMotor's velocity PID: the hub reports velocity as 16 bits (max 32,767 counts/s),
  which 8192 counts/rev overflows above ~240 RPM.
- Default command holds `targetRpm` (0..6000, coasts at 0) and `hood` (0..1). Gains in Panels under
  `Shooter.gains` (kP/kI/kD in `pidConstants`, kS/kV/kA in `ffCoefficients`), all 0.
- FullTest and TurretTest: dpad up/down ±100 RPM, right/left bumper hood ±0.05; both start at 0 RPM.
- Unmeasured: gearbox ratio to the flywheel, hood servo's safe range.

## Indicator

- goBILDA RGB Indicator Light on a servo port; shows drivers and programmers what the robot is doing.
- Actuator owner: `mechanisms/Indicator` owns the light.
- Signal (goBILDA Product Insight #4): below 1100 µs off, 1100–1900 µs a red-to-violet gradient, above 1900 µs
  white. Positions in `NextRGBIndicator.Color` assume the SDK default 600–2400 µs range. No brightness control,
  so no fading; daisy-chained lights all show the same signal.
- Patterns: solid, blink (2 Hz), fast blink (6 Hz). Idle: a glow that wanders over the red-violet range (two sine waves, 3 s and 7.3 s)
  once the OpMode starts.
  The light stays dark during INIT (no scheduler).
- No robot states yet. States go in the default command's `when`, highest priority first.
