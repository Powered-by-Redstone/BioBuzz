# Mechanisms

Physical facts the code depends on. One entry per mechanism.

## Intake

- One motor; forward pulls balls toward the indexing gate (CLAUDE.md §4.3).
- Actuator owner: `mechanisms/Intake` owns the intake motor.
- `run` at `Intake.power` (Panels-editable, default 1.0, clamped to -1.0..1.0); `reject` reverses for `rejectS`
  (calibration required, 0.5 s placeholder) and stops, also when interrupted.
- Collection (`Robot.collect`, square toggles it in FullTest; requires Intake and Gate): repeat until 4 balls
  (`Robot.MAX_BALLS`) are accepted: gate open, intake runs until a ball is within `Gate.BALL_MM`, gate hold, wait
  until the held ball matches YELLOW, RED or BLUE, at most `Gate.colorTimeoutS` (3 s, Panels; then it counts as
  unknown and is rejected like their NECTAR); POLLEN or our NECTAR: append to `Robot.balls` and schedule one `shoot` (before closing, so an interrupt can't leave a
  ball uncounted), gate close, wait `Gate.transferS` (Panels, extra to close's 0.3 s); their NECTAR or unknown: gate open, `reject`. Intake is off while the gate moves.
- Unmeasured: which motor direction pulls balls in.

## Transfer

- One motor driving the belt-coupled rollers over the transfer ramp (CLAUDE.md §4.6).
- Actuator owner: `mechanisms/Transfer` owns the transfer motor.
- `run` at `Transfer.power` (Panels-editable, default 1.0, clamped to -1.0..1.0) is the default command: spins from
  START in every OpMode (team, 2026-11-12); balls wait against the closed guard. `feed` at `feedPower` (calibration
  required, 0.5 placeholder) while a shot has the guard open. No fault handling yet.
- Unmeasured: which motor direction pushes balls toward the feeder.

## Turret

- One REV UltraPlanetary motor; motor encoder is the only feedback (relative, no absolute encoder) (CLAUDE.md §4.8).
- Actuator owner: `mechanisms/Turret` owns the turret motor.
- Reference: place the turret at the reference before powering the robot. The hub zeroes the encoder at power-up;
  the code never resets it, so the angle survives OpMode changes.
- Turret angles (radians in code, degrees in telemetry) are clockwise from the reference. Soft limits `MIN_RAD`..`MAX_RAD`
  = -60°..+60° (team, 2026-11-03); every setpoint is clamped.
- Turret axis: offset toward robot -y from the Pedro pose origin by `Turret.AXIS_Y_IN` (inches, negative), not yet
  measured; until it is set (NaN), `lookAt` holds the current angle and telemetry says so.
- Commands: `fixedAngle` (default) holds `targetRad`. An OpMode can set `defaultFromStart` during INIT to replace
  the default for that OpMode only (START takes it once); FullTest sets HIVE aiming, so the turret aims at the HIVE
  from START and returns to it after every shot. `lookAt(follower, firing, target)` sets `targetRad` toward
  `target()` (Pedro field inches, read every loop) from the turret axis, using the
  pose the chassis will have `leadS` seconds ahead (Pedro field-frame `velocity()`: vx, vy, omega) so the turret keeps
  up while the drivetrain drives and turns. `leadS` (Panels, 0 = off) is tuned by turning in place and driving past
  the target until the turret holds its bearing. `step(rad)` shifts it; dpad left/right step 10° in
  TurretTest, which starts at 0°; its circle toggles HIVE aiming. `setTarget(rad, seekRange)` (`chooseTurretAngle`) takes the
  equivalent angle (±360°) nearest the current target, unwinds the other way round when that one is past a limit, and
  parks at the nearer limit when no equivalent is reachable. While not firing (`lookAt` with `firing = false`) it
  also unwinds early: once the target is more than 180° + `seekRangeRad` (30°) from the middle of the range it jumps
  to the equivalent nearest the middle, so shots start with travel both ways; jumping back needs 2 × `seekRangeRad`
  of bearing change. This does nothing until the limits are wider than 360° + 2 × `seekRangeRad`. Unit-tested in
  `TurretAngleTest`. `targetRad` is not in Panels. `aligned`: encoder within `alignedRad` (2°) of `targetRad`.
  Gains in Panels under `Turret.gains` (kP/kI/kD in `pidConstants`, kS in `ffCoefficients`), all 0.
- Geometry assumptions (unmeasured): turret axis at the Pinpoint origin, reference facing robot forward.
- Unmeasured: gearing (`RADIANS_PER_COUNT` = 2π / (28 × gearbox ratio × turret ratio); while 0 the encoder
  always reads 0°, so any kP > 0 with a non-zero target spins the motor without stopping), usable travel beyond ±60°, and which motor direction turns clockwise.
- Limitation: PID can overshoot past the soft limits; nothing stops the motor outside them yet.

## Shooter

- One flywheel motor with velocity PID, a hood servo, and a rail servo with one position per ball type (CLAUDE.md
  §4.9, §4.10 describe the final launcher; the rail is the rail-spacing servo).
- Actuator owner: `mechanisms/Shooter` owns the flywheel motor, the hood servo and the rail servo.
- Speed is measured with the motor's own encoder, so RPM is at the motor shaft, not the flywheel. The Through Bore
  encoder can't be used with NextMotor's velocity PID: the hub reports velocity as 16 bits (max 32,767 counts/s),
  which 8192 counts/rev overflows above ~240 RPM.
- `run` holds `targetRpm` (0..6000, coasts at 0), `hood` (0..1) and the rail position for `ball`
  (`railPollen` / `railNectar` in Panels). Default command: sets `targetRpm` to 0, then `run` for the whole OpMode
  (team, 2026-11-12). Gains in Panels under `Shooter.gains` (kP/kI/kD in `pidConstants`, kS/kV/kA in
  `ffCoefficients`), all 0. `atSpeed`: target > 0 and measured RPM within `rpmTolerance` (100).
- Launch tables `POLLEN_SHOTS` / `NECTAR_SHOTS`: `Shot(distanceIn, rpm, hood)`, distance from the turret axis to the
  HIVE point, strictly increasing. `aimFor(distanceIn)` interpolates linearly for `ball`; outside the table it uses the
  nearest end entry (team, 2026-11-12); with an empty table, unsorted entries or an unknown distance it changes
  nothing and returns false (no shot). Both tables are empty until measured.
- Measuring them (TurretTest): needs Pedro and the robot's start pose (`TurretTest.startX/startY/startHeadingRad` in
  Panels, applied at START; until set, no HIVE aim or distance). Push the robot to a spot, circle aims at the HIVE,
  set RPM and hood by hand, cross fires one ball once "Ready to fire" (at speed and aligned), like `Robot.shoot` but
  without the tables or `Robot.balls`. A scoring shot gives one row: `Hive distance (in)`, target RPM, hood. TurretTest
  is also the turret PID tuner (dpad left/right steps, `Turret.gains` in Panels) and runs without Pedro.
- FullTest and TurretTest: dpad up/down ±100 RPM, right/left bumper hood ±0.05, right trigger switches `ball`
  between POLLEN and NECTAR; in FullTest a running shot overwrites RPM and hood.
- Unmeasured: gearbox ratio to the flywheel, hood servo's safe range, rail positions (both 0 for now), both launch
  tables.

## Guard

- Servo flap between the transfer and the flywheel: 1 = closed, balls wait against it while the transfer spins; 0 =
  open, the waiting ball enters the flywheel.
- Actuator owner: `mechanisms/Guard` owns the guard servo. Default command holds it closed; `open` holds it open
  and closes it when it ends, also when interrupted. `maxOpenS`: calibration required, 0.5 s placeholder.
- Shot (`Robot.shoot`, requires Turret, Guard and Transfer, priority 1, queues behind other shots in order): rail
  and table for the first ball in `Robot.balls`. The turret aims (`firing = true`) at `Robot.hive()` throughout.
  Waits with the transfer at full power while setting RPM/hood from the distance each loop, until `aimFor` covers
  the distance, `atSpeed` and `aligned`. Then opens the guard with the transfer at `Transfer.feedPower` until the
  flywheel dips (`Shooter.dipped`: more than `shotDipRpm`, at least `rpmTolerance`, below target): the ball counts as
  shot and leaves `Robot.balls`. No dip within `Guard.maxOpenS`: guard closes, nothing is counted, it waits for
  readiness and opens again. The flywheel keeps that RPM afterwards. Blocked while the HIVE state or turret axis is
  unknown. HIVE aim points: see HIVE below.
- Limitation: the dip is the only sign a ball left. A `shotDipRpm` set too high counts no shots (balls are fired but
  stay in `Robot.balls`); too low could count noise. Whether the slowed transfer keeps the next ball back until the
  guard closes must be tested on the robot.
- `Robot.balls` lives as long as the robot instance (until a Sloth reload or restart), but queued shots end with the
  OpMode. FullTest queues one shot per counted ball at START; other OpModes must do the same.

## Alliance

- `Robot.alliance` (RED until set, team 2026-11-12) is set by the Red / Blue autonomous OpModes (placeholders that do
  nothing else) and kept for TeleOp. It picks the NECTAR color to keep and the Limelight tag clusters.
- Preloads: the same autonomous OpModes call `Robot.preload()` at INIT, which sets `Robot.balls` to 4 POLLEN (manual
  §10.3.4: every robot starts with exactly 4). Assumes all 4 are in the transfer and the gate is empty (team,
  2026-11-12). Running TeleOp without an autonomous first leaves `Robot.balls` as it was, so `collect` would see the
  robot as emptier than it is.
- Both alliances use the same Pedro coordinates (team, 2026-11-12: the robot always starts at the same coordinates):
  the field is symmetric under a 180° turn (manual Figures 9-5, 9-17), so each alliance works in red's view of it.

## HIVE (manual TU06 §9.6, §9.9)

- One frame at the field center holds both HIVES side by side, red on the left as seen from the audience; HIVE
  centers 25.5 in apart. Pivots 43.95 in above the tiles. Each HIVE: two CELLS on an arm, 18.84 in gap, 12.04 in
  deep, 42.91 in overall; it rests tipped 30° with one CELL up. CELL opening 20 in wide by 14 in, 53.5 to 65.6 in
  above the tiles; it is the CELL's outer face, so it faces away from the center.
- Aim points `Robot.AUDIENCE_HIVE` (59.25, 56.2) and `COUNTER_HIVE` (59.25, 87.8): x = 72 - 25.5 / 2; y = 72 ∓ 15.8,
  the opening center's horizontal distance from the pivot (21.46 in along the arm, 5.6 in across it, at 30°).
  Assumes the team's Pedro frame is red's view from the audience (origin at the audience-side corner on red's side,
  +x toward blue, +y away from the audience) and that `HiveState.AUDIENCE` means the audience-side CELL is up.
  Neither is confirmed; swap the two points if the state means the opposite.
- AprilTags (36h11, 3.25 in, clusters of 4 under each CELL, facing the tiles): red far CELL 30-33, red audience
  CELL 34-37, blue audience CELL 38-41, blue far CELL 42-45. Seen from above with the audience at the bottom, the
  audience clusters read left to right ascending, the far clusters right to left. Blue's far cluster sits where
  red's audience cluster sits under the 180° turn, so the Limelight uses 42-45 as blue's `A` cluster.

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

## Final robot checklist

Recheck before the robot is final. Details are in the sections above and in `docs/HARDWARE.md`.

Wiring and hardware
- Every Pending row in `docs/HARDWARE.md`: ports still to wire, motor directions (intake, transfer, turret), hood
  servo hard stops, flywheel gearbox, Limelight pipeline.

Calibration (values in Panels, then copied into the companion objects)
- Pedro: `Constants.create` still returns null, so FullTest fails at INIT and TurretTest can't measure the tables. Needs the drivetrain, the Pinpoint
  localizer and Pedro's tuning.
- Start pose: the robot starts every match at the same Pedro pose (team, 2026-11-12), but it isn't measured or
  in the code yet. The autonomous needs it for `follower.setPose`; until then FullTest's field-centric heading, HIVE
  aim and distance are only right if the robot starts at the pose Pedro assumes, and TurretTest needs it typed into
  Panels every time.
- Turret gearing before PID: while `RADIANS_PER_COUNT` is 0 the encoder always reads 0°, so any kP above 0 with a
  target other than 0° turns the motor without stopping. Set the gearing before raising kP, and tune the PID with
  `MIN_RAD` / `MAX_RAD` at ±60° (or narrower).
- Turret: `RADIANS_PER_COUNT` (0 until the gearing is known), real travel for `MIN_RAD` / `MAX_RAD` (±60° now; about
  500° one way from the reference; range seeking does nothing below 420°), `AXIS_Y_IN`, PID gains (all 0),
  `alignedRad`, `leadS`.
- Shooter: PID/feedforward gains (all 0), `railPollen` / `railNectar`, `POLLEN_SHOTS` / `NECTAR_SHOTS` (empty, so
  nothing fires), `rpmTolerance`, `shotDipRpm`.
- Feeding: `Guard.maxOpenS`, `Transfer.feedPower`. Test that one guard opening lets exactly one ball through.
- Collection: `Gate.COLOR_GAIN`, the RED / YELLOW / BLUE hues, `Gate.BALL_MM`, `Gate.transferS`, `Intake.rejectS`,
  `Gate.colorTimeoutS` (3 s placeholder: long enough for a real ball to read, short enough not to stall collecting).

Assumptions to confirm
- Pedro frame: origin at the audience-side corner on red's side, +x toward blue, +y away from the audience. The HIVE
  aim points depend on it.
- `HiveState.AUDIENCE` means the audience-side CELL is up. If it means the opposite, swap `AUDIENCE_HIVE` and
  `COUNTER_HIVE`.
- Preload layout: `Robot.preload()` assumes all 4 preloaded POLLEN sit in the transfer with the gate empty. If one
  ends up held at the gate instead, preload only 3 there: `collect` then accepts the gate ball as its first, and
  counting it in `preload()` as well would count it twice.
- Field-centric driving is relative to the driver's position, with no heading-reset button (team, 2026-11-12). Each
  alliance works in red's view and the red ALLIANCE AREA is on the left from the audience (manual §9.5), so the
  drivers always stand at the x = 0 wall and the current "stick up drives toward +x" is right. Recheck it once the
  Pedro frame is confirmed. It also needs the right heading at START: FullTest sets no pose, so it treats the way
  the robot faces at START as +x; the match TeleOp must take the pose from the autonomous.

Not built yet / known gaps
- Real autonomous: Red / Blue only set the alliance and the preloads. They should shoot the preloads and hand the
  pose and turret reference over to TeleOp (CLAUDE.md §5.4; procedure not defined yet).
- Match TeleOp: must queue one shot per counted ball at START, as FullTest does.
- TeleOp run without an autonomous first doesn't load the preloads, and there is no way to set or clear
  `Robot.balls` by hand.
- Turret PID can overshoot past the soft limits; nothing stops the motor outside them (accepted: the PID is tuned
  on a narrow range).
- Indicator shows no robot states yet (for example ready to fire).
- No `Drivetrain` mechanism: the follower lives in the OpMode, so nothing stops a second writer to the drive motors.
