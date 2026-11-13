# Mechanisms

Physical facts code depends on. One entry per mechanism.

## Intake

- One motor; forward pulls balls toward indexing gate (CLAUDE.md §4.3).
- Actuator owner: `mechanisms/Intake` owns intake motor.
- `run` at `Intake.power` (Panels-editable, default 1.0, clamped to -1.0..1.0); `reject` reverses for `rejectS`
  (calibration required, 0.5 s placeholder) and stops, also when interrupted.
- Collection (`Robot.collect`, square toggles it in FullTest; requires Intake and Gate): repeat until 4 balls
  (`Robot.MAX_BALLS`) accepted: gate open, intake runs until ball within `Gate.BALL_MM`, gate hold, wait
  until held ball matches YELLOW, RED or BLUE, at most `Gate.colorTimeoutS` (3 s, Panels; then counts as
  unknown, rejected like their NECTAR); POLLEN or our NECTAR: append to `Robot.balls` and schedule one `shoot` (before closing, so interrupt can't leave
  ball uncounted), gate close, wait `Gate.transferS` (Panels, extra to close's 0.3 s); their NECTAR or unknown: gate open, `reject`. Intake off while gate moves.
- Unmeasured: which motor direction pulls balls in.

## Transfer

- One motor driving belt-coupled rollers over transfer ramp (CLAUDE.md §4.6).
- Actuator owner: `mechanisms/Transfer` owns transfer motor.
- `run` at `Transfer.power` (Panels-editable, default 1.0, clamped to -1.0..1.0) is default command: spins from
  START in every OpMode (team, 2026-11-12); balls wait against closed guard. `feed` at `feedPower` (calibration
  required, 0.5 placeholder) while shot has guard open. No fault handling yet.
- Unmeasured: which motor direction pushes balls toward feeder.

## Turret

- One REV UltraPlanetary motor; motor encoder is only feedback (relative, no absolute encoder) (CLAUDE.md §4.8).
- Actuator owner: `mechanisms/Turret` owns turret motor.
- Reference: place turret at reference before powering robot. Hub zeroes encoder at power-up;
  code never resets it, so angle survives OpMode changes.
- Turret angles (radians in code, degrees in telemetry) are clockwise from reference. Soft limits `MIN_RAD`..`MAX_RAD`
  = -60°..+60° (team, 2026-11-03); every setpoint clamped.
- Turret axis: offset toward robot -y from Pedro pose origin by `Turret.AXIS_Y_IN` (inches, negative), not yet
  measured; until set (NaN), `lookAt` holds current angle and telemetry says so.
- Commands: `fixedAngle` (default) holds `targetRad`. OpMode can set `defaultFromStart` during INIT to replace
  default for that OpMode only (START takes it once); FullTest sets HIVE aiming, so turret aims at HIVE
  from START and returns to it after every shot. `lookAt(follower, firing, target)` sets `targetRad` toward
  `target()` (Pedro field inches, read every loop) from turret axis, using
  pose chassis will have `leadS` seconds ahead (Pedro field-frame `velocity()`: vx, vy, omega) so turret keeps
  up while drivetrain drives and turns. Tune `leadS` (Panels, 0 = off) by turning in place and driving past
  target until turret holds bearing. `step(rad)` shifts it; dpad left/right step 10° in
  TurretTest, which starts at 0°; its circle toggles HIVE aiming. `setTarget(rad, seekRange)` (`chooseTurretAngle`) takes
  equivalent angle (±360°) nearest current target, unwinds other way round when that one past a limit, and
  parks at nearer limit when no equivalent reachable. While not firing (`lookAt` with `firing = false`) it
  also unwinds early: once target more than 180° + `seekRangeRad` (30°) from middle of range it jumps
  to equivalent nearest middle, so shots start with travel both ways; jumping back needs 2 × `seekRangeRad`
  of bearing change. Does nothing until limits wider than 360° + 2 × `seekRangeRad`. Unit-tested in
  `TurretAngleTest`. `targetRad` not in Panels. `aligned`: encoder within `alignedRad` (2°) of `targetRad`.
  Gains in Panels under `Turret.gains` (kP/kI/kD in `pidConstants`, kS in `ffCoefficients`), all 0.
- Geometry assumptions (unmeasured): turret axis at Pinpoint origin, reference facing robot forward.
- Unmeasured: gearing (`RADIANS_PER_COUNT` = 2π / (28 × gearbox ratio × turret ratio); while 0 encoder
  always reads 0°, so any kP > 0 with non-zero target spins motor without stopping), usable travel beyond ±60°, which motor direction turns clockwise.
- Limitation: PID can overshoot past soft limits; nothing stops motor outside them yet.

## Shooter

- One flywheel motor with velocity PID, hood servo, rail servo with one position per ball type (CLAUDE.md
  §4.9, §4.10 describe final launcher; rail is rail-spacing servo).
- Actuator owner: `mechanisms/Shooter` owns flywheel motor, hood servo and rail servo.
- Speed measured with motor's own encoder, so RPM is at motor shaft, not flywheel. Through Bore
  encoder can't be used with NextMotor's velocity PID: hub reports velocity as 16 bits (max 32,767 counts/s),
  which 8192 counts/rev overflows above ~240 RPM.
- Hood: AXON in continuous mode with analog feedback (0..360° per servo turn); hood range covers more than one
  servo turn. `periodic` unwraps reading (shorter way round, > `HOOD_MAX_STEP_DEG` per loop = lost). At code start
  reading resolved to lowest equivalent ≥ `HOOD_MIN_DEG − HOOD_MARGIN_DEG`, so hood must be near
  low end then (team, 2026-11-13); estimate kept across OpModes. `run` drives it to `hood` with NextFTC `PIDController(hoodGains)`
  plus `hoodKs·sign(error)` (form `NextMotor` position control uses), clamped to `HOOD_MIN_DEG..HOOD_MAX_DEG`. Latched faults (power 0, no shots):
  feedback jump, outside range ± margin, powered `HOOD_STALL_S` without moving 1° right way, moved `HOOD_WRONG_WAY_DEG` against its power.
- `run` holds `targetRpm` (0..6000, coasts at 0), `hood` and rail position for `ball`
  (`railPollen` / `railNectar` in Panels). Default command: sets `targetRpm` to 0, then `run` for whole OpMode
  (team, 2026-11-12). Gains in Panels under `Shooter.gains` (kP/kI/kD in `pidConstants`, kS/kV/kA in
  `ffCoefficients`), all 0. `atSpeed`: target > 0 and measured RPM within `rpmTolerance` (100). `ready`: `atSpeed`
  and hood within `hoodToleranceDeg` with no hood fault; shots wait for `ready`.
- Launch tables `POLLEN_SHOTS` / `NECTAR_SHOTS`: `Shot(distanceIn, rpm, hood)`, distance from turret axis to
  HIVE point, strictly increasing. `aimFor(distanceIn)` interpolates linearly for `ball`; outside table uses
  nearest end entry (team, 2026-11-12); with empty table, unsorted entries or unknown distance it changes
  nothing and returns false (no shot). Both tables empty until measured.
- Measuring them (TurretTest): needs Pedro and robot's start pose (`TurretTest.startX/startY/startHeadingRad` in
  Panels, applied at START; until set, no HIVE aim or distance). Push robot to a spot, circle aims at HIVE,
  set RPM and hood by hand, cross fires one ball once "Ready to fire" (at speed and aligned), like `Robot.shoot` but
  without tables or `Robot.balls`. Scoring shot gives one row: `Hive distance (in)`, target RPM, hood. TurretTest
  is also turret PID tuner (dpad left/right steps, `Turret.gains` in Panels) and runs without Pedro.
- FullTest and TurretTest: dpad up/down ±100 RPM, right/left bumper hood ±10°, right trigger switches `ball`
  between POLLEN and NECTAR; in FullTest a running shot overwrites RPM and hood.
- Unmeasured: gearbox ratio to flywheel, hood range (`HOOD_MIN_DEG`/`HOOD_MAX_DEG` NaN), hood power direction,
  Axon feedback voltage span (3.3 V assumed), rail positions (both 0 for now), both launch
  tables.

## Guard

- Servo flap between transfer and flywheel: 1 = closed, balls wait against it while transfer spins; 0 =
  open, waiting ball enters flywheel.
- Actuator owner: `mechanisms/Guard` owns guard servo. Default command holds it closed; `open` holds it open
  and closes it when it ends, also when interrupted. `maxOpenS`: calibration required, 0.5 s placeholder.
- Shot (`Robot.shoot`, requires Turret, Guard and Transfer, priority 1, queues behind other shots in order): rail
  and table for first ball in `Robot.balls`. Turret aims (`firing = true`) at `Robot.hive()` throughout.
  Waits with transfer at full power while setting RPM/hood from distance each loop, until `aimFor` covers
  distance, `atSpeed` and `aligned`. Then opens guard with transfer at `Transfer.feedPower` until
  flywheel dips (`Shooter.dipped`: more than `shotDipRpm`, at least `rpmTolerance`, below target): ball counts as
  shot and leaves `Robot.balls`. No dip within `Guard.maxOpenS`: guard closes, nothing counted, waits for
  readiness and opens again. Flywheel keeps that RPM afterwards. Blocked while HIVE state or turret axis
  unknown. HIVE aim points: see HIVE below.
- Limitation: dip is only sign a ball left. `shotDipRpm` set too high counts no shots (balls fired but
  stay in `Robot.balls`); too low could count noise. Test on robot whether slowed transfer keeps next ball back until
  guard closes.
- `Robot.balls` lives as long as robot instance (until Sloth reload or restart), but queued shots end with
  OpMode. FullTest queues one shot per counted ball at START; other OpModes must do same.

## Alliance

- `Robot.alliance` (RED until set, team 2026-11-12) set by Red / Blue autonomous OpModes (placeholders that do
  nothing else) and kept for TeleOp. Picks NECTAR color to keep and Limelight tag clusters.
- Preloads: same autonomous OpModes call `Robot.preload()` at INIT, which sets `Robot.balls` to 4 POLLEN (manual
  §10.3.4: every robot starts with exactly 4). Assumes all 4 in transfer and gate empty (team,
  2026-11-12). Running TeleOp without autonomous first leaves `Robot.balls` as it was, so `collect` would see
  robot as emptier than it is.
- Both alliances use same Pedro coordinates (team, 2026-11-12: robot always starts at same coordinates):
  field symmetric under 180° turn (manual Figures 9-5, 9-17), so each alliance works in red's view of it.

## HIVE (manual TU06 §9.6, §9.9)

- One frame at field center holds both HIVES side by side, red on left as seen from audience; HIVE
  centers 25.5 in apart. Pivots 43.95 in above tiles. Each HIVE: two CELLS on an arm, 18.84 in gap, 12.04 in
  deep, 42.91 in overall; rests tipped 30° with one CELL up. CELL opening 20 in wide by 14 in, 53.5 to 65.6 in
  above tiles; it is CELL's outer face, so faces away from center.
- Aim points `Robot.AUDIENCE_HIVE` (59.25, 56.2) and `COUNTER_HIVE` (59.25, 87.8): x = 72 - 25.5 / 2; y = 72 ∓ 15.8,
  opening center's horizontal distance from pivot (21.46 in along arm, 5.6 in across it, at 30°).
  Assumes team's Pedro frame is red's view from audience (origin at audience-side corner on red's side,
  +x toward blue, +y away from audience) and that `HiveState.AUDIENCE` means audience-side CELL is up.
  Neither confirmed; swap the two points if state means opposite.
- AprilTags (36h11, 3.25 in, clusters of 4 under each CELL, facing tiles): red far CELL 30-33, red audience
  CELL 34-37, blue audience CELL 38-41, blue far CELL 42-45. Seen from above with audience at bottom,
  audience clusters read left to right ascending, far clusters right to left. Blue's far cluster sits where
  red's audience cluster sits under 180° turn, so Limelight uses 42-45 as blue's `A` cluster.

## Indicator

- goBILDA RGB Indicator Light on servo port; shows drivers and programmers what robot is doing.
- Actuator owner: `mechanisms/Indicator` owns light.
- Signal (goBILDA Product Insight #4): below 1100 µs off, 1100–1900 µs red-to-violet gradient, above 1900 µs
  white. Positions in `NextRGBIndicator.Color` assume SDK default 600–2400 µs range. No brightness control,
  so no fading; daisy-chained lights all show same signal.
- Patterns: solid, blink (2 Hz), fast blink (6 Hz). Idle: glow that wanders over red-violet range (two sine waves, 3 s and 7.3 s)
  once OpMode starts.
  Light stays dark during INIT (no scheduler).
- Robot states (`Robot.state()`, highest priority first; telemetry "Robot state"):

  | State | Light | When |
  |---|---|---|
  | SHOOTING | white, solid | guard open |
  | REJECTING | red, fast blink | intake reversing |
  | INTAKING | blue, solid | intake pulling in |
  | NO_HIVE | orange, blink | balls waiting but Limelight sees no HIVE, so shots can't aim |
  | FULL | green, solid | 4 balls: collect stopped |
  | WAITING_TO_SHOOT | green, blink | balls waiting (flywheel spinning up or turret aligning) |
  | IDLE | wandering glow | none of above |

  States come from commanded outputs (guard position, intake throttle) and `Robot.balls`, not sensors.
  Glow passes through every gradient color, so solid state only distinct once it holds a second or two.

## Final robot checklist

Recheck before robot is final. Details in sections above and in `docs/HARDWARE.md`.

Wiring and hardware
- Every Pending row in `docs/HARDWARE.md`: ports still to wire, motor directions (intake, transfer, turret), hood
  servo hard stops, flywheel gearbox, Limelight pipeline.

Calibration (values in Panels, then copied into companion objects)
- Pedro: `Constants.create` still returns null, so FullTest fails at INIT and TurretTest can't measure tables. Needs drivetrain, Pinpoint
  localizer and Pedro's tuning.
- Start pose: robot starts every match at same Pedro pose (team, 2026-11-12), but not measured or
  in code yet. Autonomous needs it for `follower.setPose`; until then FullTest's field-centric heading, HIVE
  aim and distance only right if robot starts at pose Pedro assumes, and TurretTest needs it typed into
  Panels every time.
- Turret gearing before PID: while `RADIANS_PER_COUNT` is 0 encoder always reads 0°, so any kP above 0 with
  target other than 0° turns motor without stopping. Set gearing before raising kP, and tune PID with
  `MIN_RAD` / `MAX_RAD` at ±60° (or narrower).
- Turret: `RADIANS_PER_COUNT` (0 until gearing known), real travel for `MIN_RAD` / `MAX_RAD` (±60° now; about
  500° one way from reference; range seeking does nothing below 420°), `AXIS_Y_IN`, PID gains (all 0),
  `alignedRad`, `leadS`.
- Shooter: PID/feedforward gains (all 0), `railPollen` / `railNectar`, `POLLEN_SHOTS` / `NECTAR_SHOTS` (empty, so
  nothing fires), `rpmTolerance`, `shotDipRpm`.
- Feeding: `Guard.maxOpenS`, `Transfer.feedPower`. Test that one guard opening lets exactly one ball through.
- Collection: `Gate.COLOR_GAIN`, RED / YELLOW / BLUE hues, `Gate.BALL_MM`, `Gate.transferS`, `Intake.rejectS`,
  `Gate.colorTimeoutS` (3 s placeholder: long enough for real ball to read, short enough not to stall collecting).

Assumptions to confirm
- Pedro frame: origin at audience-side corner on red's side, +x toward blue, +y away from audience. HIVE
  aim points depend on it.
- `HiveState.AUDIENCE` means audience-side CELL is up. If opposite, swap `AUDIENCE_HIVE` and
  `COUNTER_HIVE`.
- Preload layout: `Robot.preload()` assumes all 4 preloaded POLLEN sit in transfer with gate empty. If one
  ends up held at gate instead, preload only 3 there: `collect` then accepts gate ball as its first, and
  counting it in `preload()` as well would count it twice.
- Field-centric driving is relative to driver's position, no heading-reset button (team, 2026-11-12). Each
  alliance works in red's view and red ALLIANCE AREA is on left from audience (manual §9.5), so
  drivers always stand at x = 0 wall and current "stick up drives toward +x" is right. Recheck once
  Pedro frame confirmed. Also needs right heading at START: FullTest sets no pose, so it treats the way
  robot faces at START as +x; match TeleOp must take pose from autonomous.

Not built yet / known gaps
- Real autonomous: Red / Blue only set alliance and preloads. They should shoot preloads and hand
  pose and turret reference over to TeleOp (CLAUDE.md §5.4; procedure not defined yet).
- Match TeleOp: must queue one shot per counted ball at START, as FullTest does.
- TeleOp run without autonomous first doesn't load preloads, and no way to set or clear
  `Robot.balls` by hand.
- Turret PID can overshoot past soft limits; nothing stops motor outside them (accepted: PID tuned
  on narrow range).
- Limelight aiming: turret aims from Pedro pose alone; Limelight only reports which HIVE state it sees.
  CLAUDE.md §10: refine aim with fresh tag detections (`tx`) once camera-to-turret geometry and sign
  calibrated, without fighting pose-based aim.
- No `Drivetrain` mechanism: follower lives in OpMode, so nothing stops second writer to drive motors.
