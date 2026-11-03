# Mechanisms

Physical facts the code depends on. One entry per mechanism.

## Intake

- One motor; forward pulls balls toward the indexing gate (CLAUDE.md §4.3).
- Actuator owner: `mechanisms/Intake` owns the intake motor.
- Current behavior: on/off only, at `Intake.power` (Panels-editable, default 1.0, clamped to -1.0..1.0).
  No reverse, no inventory or indexer interlock yet.
- Unmeasured: which motor direction pulls balls in.

## Turret

- One REV UltraPlanetary motor; motor encoder is the only feedback (relative, no absolute encoder) (CLAUDE.md §4.8).
- Actuator owner: `mechanisms/Turret` owns the turret motor.
- Reference: place the turret at the reference before powering the robot. The hub zeroes the encoder at power-up;
  the code never resets it, so the angle survives OpMode changes.
- Turret degrees are clockwise from the reference. Soft limits -60°..+60° (team, 2026-11-03); every setpoint is clamped.
- Commands: `fixedAngle` (default) holds `targetDeg`. `lookAt(pose)` sets `targetDeg` toward Panels
  `Turret.lookX/lookY` (field inches) from the Pedro pose. `step(deg)` shifts it; dpad left/right step 10° in
  FullTest and TurretTest, which both start at 0°. FullTest's circle toggles `lookAt`. The `targetDeg` setter wraps
  any angle into range; one past the limits parks at the nearer limit. `targetDeg` is not in Panels.
  Gains in Panels under `Turret.gains` (kP/kI/kD in `pidConstants`, kS in `ffCoefficients`), all 0.
- Geometry assumptions (unmeasured): turret axis at the Pinpoint origin, reference facing robot forward.
- Unmeasured: gearing (`DEGREES_PER_COUNT` = 360 / (28 × gearbox ratio × turret ratio); while 0 the encoder
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
