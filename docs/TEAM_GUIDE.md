# Team guide: testing, debugging, tuning

## 1. Before power-on

- **Turret reference.** Turret at reference by hand *before* power-up. Hub zeroes encoder at power-up; code never
  resets it. Turret moved while powered, or zero unsure: power off, set reference, power on.
- **Turret runaway.** `Turret.RADIANS_PER_COUNT` = `0.0`: code always reads 0°. Any `kP > 0` + target ≠ 0°: motor
  never stops, cables twist. No turret gains before 5.3.
- **Turret limits only on target.** `MIN_RAD`/`MAX_RAD` (±60° now) limit the setpoint. PID overshoot past them: nothing
  stops it. Hand on STOP while tuning.
- **Every NextFTC OpMode runs every mechanism** (shared robot, section 3). From START: transfer full power, guard
  closed, shooter writes hood + rail, turret holds last target (carried over from previous OpMode). Only *Motor test*
  and *Servo test* run just one device.
- **Control Hub servo ports 0–3 broken.** Use Control Hub servo 4–5 or Expansion Hub servos.
- **No port-addressed devices in Robot Configuration.** Intake, transfer, turret, flywheel, all servos, indicator,
  color sensor: addressed by hub + port in `Ports.kt`, not name. Also configured on same port: both drive one device,
  no warning. Named devices only: `limelight`, `frontLeft`, `frontRight`, `backLeft`, `backRight`, `pinpoint`.
- **Panels edits temporary.** Lost on Sloth deploy or app restart. Good value: copy into code (section 4), commit.

## 2. Tools

### Panels

Laptop on Control Hub Wi-Fi, open http://192.168.43.1:8001
([Panels docs](https://panels.bylazar.com/docs/com.bylazar.docs)). Graph: value vs target while tuning.
Configurables: live-edit values marked "Panels" in section 4. `const val` / `val` not shown: code change.

### Unit tests

Tested math: turret angle choice (`TurretAngleTest`), launch-table interpolation (`ShotTableTest`), in
`src/test/java`. Run after changing `chooseTurretAngle`, `turretAngleTo` or `interpolate`. No robot needed:

```bash
./gradlew testDebugUnitTest
```

## 3. How it works

- **One robot, all OpModes.** NextFTC creates `Robot` once; shared until Sloth deploy or app restart.
  `Robot.alliance`, `Robot.balls` (count + order), turret target, Panels values carry over between OpModes.
- **Collecting** (`Robot.collect`): gate open, intake runs until ball within `Gate.BALL_MM`, gate holds, color read
  (max `Gate.colorTimeoutS`). Yellow or our alliance color: counted, one shot queued, gate closes, ball into transfer.
  Else: gate opens, intake reverses `Intake.rejectS`. Stops at 4 balls.
- **Shooting** (`Robot.shoot`): automatic, one per counted ball, in order. Turret aims at HIVE; RPM + hood from launch
  table for ball type + distance. Table covers distance, flywheel within `rpmTolerance`, turret within `alignedRad`:
  guard opens, transfer slowed to `feedPower`. Flywheel dip of `shotDipRpm` = ball fired. No dip within
  `Guard.maxOpenS`: guard closes, retries.
- **Indicator**, highest priority first: white solid = shooting, red fast blink = rejecting, blue solid = intaking,
  orange blink = balls waiting, no HIVE seen, green solid = full (4), green blink = waiting to shoot, red-violet glow =
  idle. Dark in INIT: normal.

## 4. What to change

"Panels" = live-edit under class name in Panels Configurables, then copy final value into same field in code.
"Code" = edit file, redeploy.

### Ports (`Ports.kt`, code)

Ports set by team in `Ports.kt` (2026-11-13), not yet checked against wiring. Check each, tick off in checklist
(section 8).

Change port: number in `Ports.kt`. Other hub: move constant to other object (`ControlHub` ↔ `ExpansionHub`)
*and* mechanism passes other hub (`RobotController.controlHub` ↔ `RobotController.expansionHub`). Update
`docs/HARDWARE.md`.

Color sensor code: REV Color Sensor **V2** only. V3 needs code change.

### Motor directions (code)

No direction set in code; all FORWARD (team verified directions, 2026-11-13). Intake, transfer: flip in Panels with
negative power. Turret, flywheel: code change, `direction = REVERSE` on that `NextMotor` (flips power + encoder). **Never flip turret or flywheel with negative
`RADIANS_PER_COUNT`**: flips encoder only, PID pushes wrong way, motor runs away.

### Tunables

| Value | Where | Now | Unit / meaning | Step |
|---|---|---|---|---|
| `Intake.power` | Panels | 1.0 | -1..1; negative flips intake | 5.1 |
| `Intake.rejectS` | Panels | 0.5 | s reversing to push rejected ball out | 5.4 |
| `Transfer.power` | Panels | 1.0 | -1..1; negative flips transfer | 5.1 |
| `Transfer.feedPower` | Panels | 0.5 | -1..1 while guard open | 5.7 |
| `Gate.OPEN_POS` / `HOLD_POS` / `CLOSE_POS` | Panels | 0.17 / 0.32 / 0.58 | servo 0..1 | 5.2 |
| `Gate.COLOR_GAIN` | Panels | 15 | software gain, color sensor | 5.4 |
| `Gate.BALL_MM` | Panels | 14 | mm; closer = ball at gate | 5.4 |
| `Gate.transferS` | Panels | 0 | s extra wait after closing | 5.4 |
| `Gate.colorTimeoutS` | Panels | 3 | s before unreadable ball rejected | 5.4 |
| `Gate.RED` / `YELLOW` / `BLUE` | code | hue 356 / 20 / 190 | HSV ball colors (± 25° hue, ± 0.4 saturation) | 5.4 |
| `Guard.CLOSED` / `OPEN` | code | 0.55 / 0.78 | servo 0..1 | 5.2 |
| `Guard.maxOpenS` | Panels | 0.5 | s guard stays open waiting for dip | 5.7 |
| `Turret.RADIANS_PER_COUNT` | code | 0.0 | turret rad per motor encoder count | 5.3 |
| `Turret.MIN_RAD` / `MAX_RAD` | Panels | ±π/3 (±60°) | rad, clockwise from reference | 5.3 |
| `Turret.gains` | Panels | all 0 | `pidConstants` kP/kI/kD, `ffCoefficients` kS | 5.5 |
| `Turret.alignedRad` | Panels | π/90 (2°) | rad; shots wait until this close | 5.5 |
| `Turret.AXIS_Y_IN` | Panels | NaN | in, turret axis left(+)/right(−) of Pinpoint origin | 5.8 |
| `Turret.leadS` | Panels | 0 | s of chassis motion to aim ahead; 0 = off | 5.10 |
| `Turret.STEP_RAD` | Panels | π/18 (10°) | dpad step, TurretTest | – |
| `Turret.seekRangeRad` | Panels | π/6 (30°) | early unwind; only matters once limits exceed 420° | – |
| `Shooter.MAX_RPM` | code | 2100 | RPM cap at encoder shaft | 5.1 |
| `Shooter.RADIANS_PER_COUNT` | code | 2π/764 | motor encoder, 764 counts/rev; RPM at encoder shaft | – |
| `Shooter.gains` | Panels | all 0 | `pidConstants` kP/kI/kD, `ffCoefficients` kS/kV/kA | 5.6 |
| `Shooter.rpmTolerance` | Panels | 100 | RPM; shots wait until this close | 5.6 |
| `Shooter.shotDipRpm` | Panels | 300 | RPM drop = ball fired | 5.7 |
| `Shooter.railPollen` / `railNectar` | Panels | 0 / 0 | rail servo 0..1 per ball type | 5.2 |
| `Shooter.POLLEN_SHOTS` / `NECTAR_SHOTS` | code | empty | launch tables; empty = never fires | 5.9 |
| `Limelight.PIPELINE` | code | 0 | pipeline index | 5.8 |
| `Robot.AUDIENCE_HIVE` / `COUNTER_HIVE` | code | (59.25, 56.2) / (59.25, 87.8) | Pedro inches, aim point per HIVE state | 5.8 |
| Pedro drive + localizer | `pedro/Constants.java` | configs filled, `create()` returns null | see 5.8 | 5.8 |

**Committing turret/shooter gains.** Companion `gains = MotorPositionConstants()` (`MotorVelocityConstants()` in
`Shooter`) is only a placeholder so Panels has a field to show. `init` then points `gains` at the motor's own
constants (`gains = motor.positionConstants` / `motor.velocityConstants`): the object the motor's PID reads, so Panels
edits reach the PID. Placeholder discarded; anything written in it never used. Other way round
(`motor.positionConstants = gains`) impossible: `val` in NextFTC, and the PID grabs it when the motor is created.
Commit tuned gains in `init`, after that line, e.g. `gains.kP = 0.8` (also `kI`, `kD`, `kS`, `kV`, `kA`).
`pidConstants`, `ffCoefficients` are `internal`: Panels shows them, Kotlin code can't reach them.

## 5. Bring-up and tuning, in order

Each step on blocks or robot held first, then floor. No skipping: later steps need earlier values.

### 5.1 Motor directions, gearing — *Motor test*

Plain SDK OpMode: one motor on gamepad 1 left stick, nothing else runs. Set `MotorTest.expansionHub` +
`MotorTest.port` (from `Ports.kt`) in Panels **before INIT**. Telemetry: power, position (counts), velocity
(counts/s).

1. **Intake**: stick up pulls balls in. Else `Intake.power` = -1.0.
2. **Transfer**: stick up moves balls toward flywheel. Else `Transfer.power` + `Transfer.feedPower`
   negative.
3. **Turret**, cables slack, small stick moves:
   - Stick up: **clockwise seen from above**, position **increases**. Counter-clockwise: `direction = REVERSE` (code).
     Position decreases with positive power: stop. Motor + encoder disagree, `REVERSE` won't fix, PID can't work.
     Check encoder cable.
   - Gearing: mark start, turn exactly 90° with stick, read count change. `RADIANS_PER_COUNT = (π / 2) / counts`
     (= 2π / (28 × gearbox ratio × turret gear ratio)).
   - Measure real safe travel from reference, both directions (expected ~500° one way). Needed for 5.3.
4. **Flywheel**: stick up spins launch direction **and** velocity positive. Else
   `direction = REVERSE` (code). Note top velocity at full stick: RPM = counts/s × 60 / 764. `MAX_RPM` (2100)
   below safe top speed (code).

### 5.2 Servo positions — *Servo test*

Plain SDK OpMode: dpad up/down steps one servo by `ServoTest.step` (0.03). Set `ServoTest.expansionHub`, `port`,
`step` in Panels before INIT (hub + port from `Ports.kt`). **At START servo jumps to 0.5**: 0.5 safe for linkage,
or unhook first. Limp at STOP.

| Servo | Find |
|---|---|
| Gate | `OPEN_POS` (ball passes in), `HOLD_POS` (ball held still at sensor), `CLOSE_POS` (ball pushed into transfer) |
| Guard | `CLOSED` (0.55) holds balls, `OPEN` (0.78) lets one through? Else change `Guard.CLOSED` / `OPEN` (code) |
| Hood | range without hard stops. Code clamps 0..1 only; narrower safe range: change clamp in `Shooter.hood` (code) |
| Rail | `railPollen`, `railNectar` (rail spacing per ball) |

Indicator: not a positional servo, skip.

### 5.3 Turret gearing, limits (code)

1. Measured `RADIANS_PER_COUNT` into `Turret.kt`, deploy.
2. `MIN_RAD` / `MAX_RAD` stay ±60° (or narrower) for PID tuning.
3. Only after PID works (5.5): measured travel minus safety margin each side. `MIN_RAD` = counter-clockwise travel
   from reference (negative, likely near 0), `MAX_RAD` = clockwise travel, radians. Code then unwinds automatically
   when HIVE out of reach.

### 5.4 Collecting — *Gate test*, then *Full test*

*Gate test* (NextFTC: transfer spins from START): cross = open, square = close, triangle = hold, circle = open, wait
for yellow ball within 160 mm, hold, close. Telemetry: `Gate position`, `Gate sensor output` (raw color/distance).

1. Check three gate positions with ball by hand.
2. **Distance**: ball resting against gate, read distance in `Gate sensor output`. `BALL_MM` slightly above. No ball:
   distance well above `BALL_MM`.
3. **Gain**: brightest ball held, raise `COLOR_GAIN` until largest channel ~200 of 255. Too low: hue garbage. No
   channel at 255.
4. **Hues**: each ball type, competition-like lighting, note hue + saturation. Edit `Gate.RED` / `YELLOW` / `BLUE`
   (+ tolerance) in `Gate.kt` (code, not Panels). Hue tolerance ±25°: profiles >50° apart.
5. Real collection: square in *Full test* (needs Pedro, 5.8). No autonomous INIT first: counts 4 preloads, collecting
   stops at 4. Watch indicator. Yellow + our NECTAR kept (blue, then into transfer); other NECTAR rejected (fast red).
   Tune `Intake.rejectS` until rejected ball fully out, `Gate.transferS` if kept ball misses transfer before gate
   reopens, `colorTimeoutS` short enough not to stall.
6. Repeated control of opponent NECTAR = penalty (G408), even with auto-reject. Reject fast.

### 5.5 Turret PID — *Turret test*

Controls: dpad left/right = target ∓10°; dpad up/down = RPM ±50; bumpers = hood ±0.05; right trigger = POLLEN/NECTAR;
circle = toggle HIVE aiming (needs Pedro); cross = fire one ball. Target 0° at INIT. Works without Pedro.

1. Needs: 5.3 step 1 done, turret at reference at power-up.
2. Panels graph: `Turret deg`, `Turret target deg`.
3. `Turret.gains.pidConstants`: small `kP`, step with dpad. Raise `kP` until fast, small overshoot.
4. `kD` damps overshoot. Stops short: little `kS` (`ffCoefficients`). `kI` only for steady error.
5. Must settle within `alignedRad` (2°) every step, else shots never fire.
6. Commit gains (section 4, "Committing turret/shooter gains").

### 5.6 Flywheel PID — *Turret test*

1. Graph `Shooter rpm`, `Shooter target rpm`. Raise target with dpad up.
2. `Shooter.gains` all 0: raise `ffCoefficients.kV` until RPM near target at several speeds. Low speeds lag: `kS`.
3. `pidConstants.kP` removes rest of error, fast recovery after shot. Too much: oscillates.
4. Must hold within `rpmTolerance` (100). Commit gains.

### 5.7 Single-ball feeding — *Turret test*

Balls into transfer by hand (runs from START). RPM that throws safely. Cross fires one ball once `Ready to fire`
(flywheel at speed, turret aligned).

1. Watch `Shooter rpm` dip during shot. `shotDipRpm` ≈ half the dip, above noise. Too high: no shot detected (match:
   guard keeps reopening). Too low: noise = shot.
2. `Guard.maxOpenS`: bit longer than guard-open-to-dip time.
3. `Transfer.feedPower`: lower until exactly one ball per opening. Two balls at any setting: mechanical (guard closes
   too slow). Report it, don't hide in software.

### 5.8 Pedro, start pose, HIVE, Limelight

**Pedro** (needed for HIVE aiming, Full test, launch tables). `pedro/Constants.java` `create()` returns `null`:
*Full test* crashes at INIT, *Turret test* can't aim at HIVE. Drive + Pinpoint configs filled (`driveConfig`,
`localizerConfig`). Tuners registered in `pedro/Tuning.java`: `mecanumTuner`, `pinpointTuner`, `foresightTuner`,
`driveTest`, `localizationTest`. Tune per [Pedro docs](https://pedropathing.com/docs) and
[Pinpoint tuning](https://pedropathing.com/docs/pathing/tuning/localization/pinpoint), then make `Constants.create`
build the `Follower` from the configs.

**Start pose.** Same every match, not measured yet. Pedro frame (assumed, unconfirmed): origin audience-side corner on
red's side, +x toward blue, +y away from audience, inches, radians counter-clockwise. Blue uses same numbers (field
symmetric under 180° turn). *Turret test*: `TurretTest.startX/startY/startHeadingRad` in Panels, before START every
time.

**Turret axis.** `AXIS_Y_IN`: turret rotation axis sideways offset from Pinpoint origin, inches, + = left, − = right.
NaN until set: HIVE aiming holds still (telemetry says so). Code also assumes axis level with Pinpoint origin
front-to-back and reference pointing straight ahead. Either false: aiming math needs code change.

**Limelight.**
1. Limelight into blue USB 3.0 port on Control Hub. Driver Station, Configure Robot, scan: "Ethernet Device" appears.
   Name `limelight`, save.
2. Pipeline over Wi-Fi: robot app running, laptop on Control Hub Wi-Fi, open http://192.168.43.1:5801 (Panels
   Limelight proxy forwards to camera; stream on port 5800). ~20 s after power-up for green light. Pipeline selector:
   **0**. Then:
   - Input tab: Pipeline Type = Fiducial Markers, Black Level = 0, 640x480 (code needs 2D only), Gain ~15, exposure as
     low as still shows tags clearly (less blur).
   - Standard tab: Family = AprilTag Classic 36h11, Marker Size = 82.55 mm (3.25 in). ID Filters empty or 30–45. Code
     uses both alliances' IDs (30–45): don't filter to one ID.
   - Tags visible in stream? Reconnect once: settings kept?

   Details: [Limelight 3A setup](https://docs.limelightvision.io/docs/docs-limelight/getting-started/limelight-3a),
   [AprilTag pipelines](https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltags).
3. *Limelight test* (no controls): `Hive state` AUDIENCE, COUNTER or NA. Alliance from last autonomous INIT (RED
   default; INIT Blue autonomous to test blue).
4. Tilt real HIVE each way. `AUDIENCE` = audience-side CELL up. Opposite: swap `Robot.AUDIENCE_HIVE` /
   `COUNTER_HIVE` (code).
5. No tags visible: NA, no firing.

### 5.9 Launch tables — *Turret test* (needs Pedro, start pose, `AXIS_Y_IN`, Limelight seeing HIVE)

Aim point from `Hive state`. NA: turret holds still, distance NaN.

1. Start pose into Panels during INIT, robot there, START.
2. Push robot to shooting spot. Circle: turret aims at HIVE. `Hive distance (in)` = distance.
3. Right trigger: ball type (`Rail ball`). RPM (dpad up/down), hood (bumpers), cross fires. Repeat until scores
   reliably.
4. Row: `Shot(Hive distance (in), Shooter target rpm, Hood position)` into `POLLEN_SHOTS` or `NECTAR_SHOTS` in
   `Shooter.kt`.
5. Whole shooting area, ~every 12 in. Distances strictly increasing; unsorted table blocks firing. Between rows:
   interpolated. Beyond ends: end row.

### 5.10 Full match behavior — *Full test*

Controls: left stick drives (field-centric: stick up = +x, away from drivers), right stick turns, square toggles
collecting, dpad up/down RPM, bumpers hood, right trigger ball type (running shot overwrites RPM + hood). Turret aims
at HIVE from START. One shot queued at START per counted ball.

1. INIT Red or Blue autonomous first (INIT enough): sets alliance, counts 4 preloaded POLLEN.
2. No start pose in *Full test*: direction robot faces at START = +x.
3. Expect: preloads fire, square collects up to 4, kept balls fire automatically.
4. `leadS`: turn in place, drive past HIVE; raise until turret holds bearing while moving.

## 6. OpModes

| OpMode | Type | For | Needs |
|---|---|---|---|
| Motor test | Utility, plain SDK | one motor by port: direction, encoder | hub/port in Panels before INIT |
| Servo test | Utility, plain SDK | one servo by port: positions | hub/port/step in Panels before INIT |
| Gate test | Utility | gate positions, color/distance sensor | – |
| Limelight test | Utility | HIVE state from AprilTags | Limelight configured |
| Turret test | Utility | turret PID, flywheel PID, feeding, launch tables | Pedro, start pose, Limelight only for HIVE aiming |
| Full test | Utility | match-like TeleOp | Pedro (crashes at INIT without) |
| Red / Blue | Autonomous | **placeholders**: set alliance, count 4 preloads only | – |

Not built: real autonomous (drive, shoot preloads, hand pose + turret reference to TeleOp), match TeleOp, drivetrain
mechanism, Limelight aim correction, manual set/clear of ball count. See "Final robot checklist" in
`docs/MECHANISMS.md`.

## 7. Debugging

Telemetry keys: `Alliance`, `Balls` (count + order, first = next shot), `Robot state`, `Hive state`,
`Gate position`, `Gate sensor output`, `Intake throttle`, `Transfer throttle`, `Guard position`, `Turret deg`,
`Turret target deg`, `Shooter rpm`, `Shooter target rpm`, `Hood position`, `Rail ball`. Test OpModes add
`Ready to fire`, `Hive distance (in)`, pose `x (in)`, `y (in)`, `heading (deg)`.

| Symptom | Check |
|---|---|
| Full test crashes at INIT | Pedro not configured (`Constants.create` returns null) |
| Device dead / wrong device moves | `Ports.kt` vs wiring; Control Hub servo 0–3 dead; device also in Robot Configuration on that port |
| Turret spins, won't stop | `RADIANS_PER_COUNT` still 0, wrong motor direction, encoder unplugged. STOP, power off, turret to reference |
| Turret angle drifts off | moved by hand while powered, or not at reference at power-up. Power-cycle at reference |
| Turret not aiming at HIVE | `AXIS_Y_IN` NaN (telemetry says), `Hive state` NA, no start pose (Turret test), no Pedro |
| Turret aims at wrong CELL | swap `AUDIENCE_HIVE` / `COUNTER_HIVE`; or Pedro frame assumption wrong |
| Never fires (`Balls` not empty) | in order: launch table empty/unsorted; `Hive state` NA (orange blink); `AXIS_Y_IN` NaN (`Hive distance` NaN); RPM never within `rpmTolerance` (flywheel gains); turret never within `alignedRad` (turret gains) |
| Guard opens/closes repeatedly, `Balls` not shrinking | `shotDipRpm` too high, or `maxOpenS` too short |
| Two balls per shot | lower `feedPower`; else mechanical (guard closes too slow) |
| Every ball rejected / unknown | `COLOR_GAIN`, hue profiles, `BALL_MM`; read `Gate sensor output` |
| Square does nothing, Full test | already 4 counted (green solid); preloads counted at autonomous INIT |
| `Balls` wrong after redeploy/restart | count resets to empty; INIT autonomous to recount preloads |
| Alliance wrong | default RED; INIT right autonomous first |
| Panels value reverted | Panels edits lost on deploy/restart; commit in code |
| Indicator dark | normal in INIT; else check wiring + port in `Ports.kt` |

## 8. Checklist

Done on robot + value in code: tick (`- [x]`) in a commit. GitHub shows checkboxes; ticking only by editing file.

### Wiring and configuration

- [ ] Intake motor: port set in `Ports.kt`, wired to match
- [ ] Transfer motor: port set in `Ports.kt`, wired to match
- [ ] Indicator light: port set in `Ports.kt`, wired to match
- [ ] Gate servo: port set in `Ports.kt`, wired to match
- [ ] Gate color sensor (REV V2, I2C): port set in `Ports.kt`, wired to match
- [ ] Turret motor + encoder: port set in `Ports.kt`, wired to match
- [ ] Flywheel motor + encoder: port set in `Ports.kt`, wired to match
- [ ] Hood servo: port set in `Ports.kt`, wired to match
- [ ] Rail servo: port set in `Ports.kt`, wired to match
- [ ] Guard servo: port set in `Ports.kt`, wired to match
- [ ] Limelight in Control Hub USB 3.0 port, configured as `limelight`
- [ ] Drive motors (`frontLeft`, `frontRight`, `backLeft`, `backRight`) + `pinpoint` in Robot Configuration
- [ ] No port-addressed device also in Robot Configuration

### Directions and gearing (5.1)

- [ ] Intake pulls balls in (`Intake.power` sign)
- [ ] Transfer moves balls toward flywheel (`Transfer.power`, `feedPower` sign)
- [ ] Turret: positive power clockwise, encoder counts up
- [ ] Turret `RADIANS_PER_COUNT` measured
- [ ] Turret safe travel measured both ways from reference
- [ ] Flywheel: positive power launch direction, velocity positive
- [ ] Flywheel motor model checked against `MAX_RPM`

### Servos (5.2)

- [ ] Gate `OPEN_POS`, `HOLD_POS`, `CLOSE_POS`
- [ ] Guard `CLOSED` / `OPEN`
- [ ] Hood safe range (clamp changed if narrower than 0..1)
- [ ] `railPollen`, `railNectar`

### Collecting (5.4)

- [ ] `BALL_MM`
- [ ] `COLOR_GAIN`
- [ ] `RED`, `YELLOW`, `BLUE` hues measured, competition lighting
- [ ] `rejectS`, `transferS`, `colorTimeoutS`
- [ ] Right balls kept + rejected, both alliances

### Turret and flywheel (5.3, 5.5, 5.6)

- [ ] Turret gains
- [ ] `MIN_RAD` / `MAX_RAD` = measured travel
- [ ] Flywheel gains
- [ ] `alignedRad`, `rpmTolerance` reached reliably

### Feeding (5.7)

- [ ] `shotDipRpm`, `Guard.maxOpenS`
- [ ] `feedPower`: exactly one ball per guard opening

### Pedro, HIVE and Limelight (5.8)

- [ ] Pedro set up + tuned, `Constants.create` filled
- [ ] Pedro frame confirmed (origin, axes)
- [ ] Start pose measured
- [ ] `AXIS_Y_IN` measured
- [ ] Limelight pipeline 0 set up
- [ ] `Hive state` matches real HIVE (else swap `AUDIENCE_HIVE` / `COUNTER_HIVE`)
- [ ] No tags: `Hive state` NA, no firing

### Shooting and match behavior (5.9, 5.10)

- [ ] `POLLEN_SHOTS` measured
- [ ] `NECTAR_SHOTS` measured
- [ ] `leadS`
- [ ] *Full test*: preloads fire, collecting fills to 4, kept balls fire automatically
