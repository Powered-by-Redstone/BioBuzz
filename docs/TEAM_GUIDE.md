# Team guide: testing, debugging and tuning

## 1. Must-know before you power anything

- **Turret reference.** Turn the turret by hand to its reference position *before* powering the robot. The hub zeroes
  the turret encoder at power-up and the code never resets it. If the turret was moved by hand while powered, or you
  are unsure where zero is: power off, put it at the reference, power on.
- **Turret runaway.** `Turret.RADIANS_PER_COUNT` is `0.0`, so the code always reads the turret at 0°. With any
  `kP > 0` and a target other than 0°, the turret motor turns without stopping and twists the cables. Do not raise turret
  gains before step 5.3 is done.
- **Turret limits are only on the target.** `MIN_RAD`/`MAX_RAD` (±60° now) limit where the turret is *told* to go.
  Nothing stops the motor if the PID overshoots past them. Keep a hand on STOP while tuning.
- **Every NextFTC OpMode runs every mechanism** (shared robot, section 3): from START the transfer spins at full
  power, the guard holds closed, the shooter writes its hood and rail positions, and the turret holds its last
  target, which carries over from the previous OpMode. Only *Motor test* and *Servo test* run nothing but the one
  device.
- **Control Hub servo ports 0–3 are broken.** Use Control Hub servo ports 4–5 or Expansion Hub servo ports.
- **Do not add port-addressed devices to the Robot Configuration.** Intake, transfer, turret, flywheel, all servos,
  the indicator and the color sensor are addressed by hub and port in `Ports.kt`, not by name. If something is also
  configured on the same port, both drive one device and nothing warns you. The only named devices are
  `limelight` (and, once Pedro is set up, the four drive motors and the Pinpoint).
- **Panels edits are temporary.** Values changed in Panels are lost at the next Sloth deploy or app restart. When a
  value is good, copy it into the code (section 4) and commit it.

## 2. Tools

### Panels (dashboard)

Connect the laptop to the Control Hub's Wi-Fi and open http://192.168.43.1:8001
([Panels docs](https://panels.bylazar.com/docs/com.bylazar.docs)). Use Graph to plot a value against its target
while tuning, and Configurables to live-edit the values marked "Panels" in section 4. `const val` and `val` values
don't appear there and need a code change.

### Unit tests

Pure math with a hardware risk is unit-tested: turret angle choice (`TurretAngleTest`) and launch-table interpolation
(`ShotTableTest`), in `src/test/java`. Run them after changing `chooseTurretAngle`, `turretAngleTo` or
`interpolate` (no robot needed):

```bash
./gradlew testDebugUnitTest
```

## 3. How the code is organized

```text
src/main/java/org/firstinspires/ftc/teamcode/
  Ports.kt         hub port of every device (ControlHub / ExpansionHub objects)
  Robot.kt         the one shared robot: mechanisms, ball count, alliance, collect(), shoot(), HIVE aim points
  mechanisms/      one file per mechanism; tunables in each file's companion object
  opModes/         OpModes; Controls.kt has every gamepad binding
  pedro/           Pedro Pathing setup (Constants.create) and Pedro's tuning procedures
```

- **One robot for all OpModes.** NextFTC creates `Robot` once and every OpMode shares it until the next Sloth
  deploy or app restart. So `Robot.alliance`, `Robot.balls` (the ball count and order), the turret target and
  Panels values all carry over from one OpMode to the next.
- **Mechanisms** own hardware: `Intake`, `Gate` (indexing servo + color/distance sensor), `Transfer`, `Guard` (flap
  before the flywheel), `Shooter` (flywheel, hood servo, rail servo), `Turret`, `Limelight`, `Indicator` (RGB light).
  Each logs its actual and target values every loop.
- **Collecting** (`Robot.collect`): gate opens, intake runs until a ball is within `Gate.BALL_MM`, gate holds it,
  the color is read (max `Gate.colorTimeoutS`). Yellow or our alliance color: counted, one shot queued, gate closes
  to push it into the transfer. Anything else: gate opens and the intake reverses for `Intake.rejectS`. Stops at
  4 balls.
- **Shooting** (`Robot.shoot`), fully automatic, one per counted ball, in order: the turret aims at the HIVE, RPM and
  hood come from the launch table for the ball's type and distance. When the table covers the distance, the flywheel
  is within `rpmTolerance` and the turret within `alignedRad`, the guard opens with the transfer slowed to
  `feedPower`. A flywheel dip of `shotDipRpm` counts the ball as fired. No dip within `Guard.maxOpenS`: the guard
  closes and it tries again.
- **Indicator light**, highest priority first: white solid = shooting, red fast blink = rejecting, blue solid =
  intaking, orange blink = balls waiting but no HIVE seen, green solid = full (4 balls), green blink = waiting to
  shoot, wandering red-violet glow = idle. Dark during INIT is normal.

## 4. Everything you may need to change

"Panels" = live-editable under the class name in Panels Configurables; copy the final value into the same field
in the code. "Code" = edit the file and redeploy.

### Ports (`Ports.kt`, code)

| Device | Hub, port | Constant | Status |
|---|---|---|---|
| Intake motor | Control Hub, motor 2 | `ControlHub.INTAKE_MOTOR` | confirmed |
| Transfer motor | Control Hub, motor 3 | `ControlHub.TRANSFER_MOTOR` | wire to match |
| Indicator light | Control Hub, servo 4 | `ControlHub.INDICATOR_SERVO` | wire to match |
| Gate servo | Control Hub, servo 5 | `ControlHub.GATE_SERVO` | wire to match |
| Gate color sensor (REV V2) | Control Hub, I2C bus 2 | `ControlHub.GATE_COLOR_SENSOR_I2C_BUS` | confirmed |
| Turret motor | Expansion Hub, motor 2 | `ExpansionHub.TURRET_MOTOR` | confirmed |
| Flywheel motor | Expansion Hub, motor 3 | `ExpansionHub.SHOOTER_MOTOR` | confirmed |
| Hood servo | Expansion Hub, servo 0 | `ExpansionHub.HOOD_SERVO` | confirmed |
| Rail servo | Expansion Hub, servo 1 | `ExpansionHub.RAIL_SERVO` | wire to match |
| Guard servo | Expansion Hub, servo 2 | `ExpansionHub.GUARD_SERVO` | confirmed |
| Limelight 3A | config name `limelight` | in `Limelight.kt` | add to Robot Configuration |

To move a device: change the number in `Ports.kt`. A device moved to the other hub must also move to the other
object (`ControlHub` ↔ `ExpansionHub`) *and* its mechanism must pass the other hub (`RobotController.controlHub` ↔
`RobotController.expansionHub`). Update `docs/HARDWARE.md`.

The color sensor code only supports a REV Color Sensor **V2**. A V3 needs a code change.

### Motor directions (code)

Nothing sets a motor direction yet; every motor runs FORWARD. Intake and transfer can be flipped in Panels by making
their power negative. Turret and flywheel need a code change: set `direction = REVERSE` on that `NextMotor`, which
flips power and encoder together. **Never flip the turret or flywheel by making `RADIANS_PER_COUNT` negative**: that
flips only the encoder, the PID then pushes the wrong way and the motor runs away.

### Tunables

| Value | Where | Now | Unit / meaning | Found in |
|---|---|---|---|---|
| `Intake.power` | Panels | 1.0 | -1..1; negative flips the intake | 5.1 |
| `Intake.rejectS` | Panels | 0.5 | s reversing to push a rejected ball out | 5.4 |
| `Transfer.power` | Panels | 1.0 | -1..1; negative flips the transfer | 5.1 |
| `Transfer.feedPower` | Panels | 0.5 | -1..1 while the guard is open | 5.7 |
| `Gate.OPEN_POS` / `HOLD_POS` / `CLOSE_POS` | Panels | 0.35 / 0.5 / 0.55 | servo 0..1 | 5.2 |
| `Gate.COLOR_GAIN` | Panels | 20 | software gain on the color sensor | 5.4 |
| `Gate.BALL_MM` | Panels | 160 | mm; closer = ball at the gate | 5.4 |
| `Gate.transferS` | Panels | 0 | s extra wait after closing | 5.4 |
| `Gate.colorTimeoutS` | Panels | 3 | s before an unreadable ball is rejected | 5.4 |
| `Gate.RED` / `YELLOW` / `BLUE` | code | hue 0 / 60 / 220 | HSV ball colors (± 25° hue, ± 0.4 saturation) | 5.4 |
| `Guard.CLOSED` / `OPEN` | code | 1.0 / 0.0 | servo 0..1 | 5.2 |
| `Guard.maxOpenS` | Panels | 0.5 | s the guard may stay open waiting for a dip | 5.7 |
| `Turret.RADIANS_PER_COUNT` | code | 0.0 | turret radians per motor encoder count | 5.3 |
| `Turret.MIN_RAD` / `MAX_RAD` | Panels | ±π/3 (±60°) | rad, clockwise from the reference | 5.3 |
| `Turret.gains` | Panels | all 0 | `pidConstants` kP/kI/kD, `ffCoefficients` kS | 5.5 |
| `Turret.alignedRad` | Panels | π/90 (2°) | rad; shots wait until this close | 5.5 |
| `Turret.AXIS_Y_IN` | Panels | NaN | in, turret axis left(+)/right(−) of the Pinpoint origin | 5.8 |
| `Turret.leadS` | Panels | 0 | s of chassis motion to aim ahead; 0 = off | 5.10 |
| `Turret.STEP_RAD` | Panels | π/18 (10°) | dpad step in TurretTest | – |
| `Turret.seekRangeRad` | Panels | π/6 (30°) | early unwind; only matters once limits exceed 420° | – |
| `Shooter.MAX_RPM` | code | 6000 | motor-shaft RPM cap | 5.1 |
| `Shooter.RADIANS_PER_COUNT` | code | 2π/28 | bare motor encoder; RPM is at the motor shaft | – |
| `Shooter.gains` | Panels | all 0 | `pidConstants` kP/kI/kD, `ffCoefficients` kS/kV/kA | 5.6 |
| `Shooter.rpmTolerance` | Panels | 100 | RPM; shots wait until this close | 5.6 |
| `Shooter.shotDipRpm` | Panels | 300 | RPM drop that counts as a fired ball | 5.7 |
| `Shooter.railPollen` / `railNectar` | Panels | 0 / 0 | rail servo 0..1 per ball type | 5.2 |
| `Shooter.POLLEN_SHOTS` / `NECTAR_SHOTS` | code | empty | launch tables; empty = never fires | 5.9 |
| `Limelight.PIPELINE` | code | 0 | Limelight pipeline index | 5.8 |
| `Robot.AUDIENCE_HIVE` / `COUNTER_HIVE` | code | (59.25, 56.2) / (59.25, 87.8) | Pedro inches, aim point per HIVE state | 5.8 |
| Pedro drive and localizer | `pedro/Constants.java` | returns null | see 5.8 | 5.8 |

**Committing turret or shooter gains.** `Turret.gains` and `Shooter.gains` are replaced by the motor's own
constants when the mechanism is created, so a value typed into the companion object is overwritten. To commit tuned
gains, set them in the mechanism's `init` block after the `gains = motor...` line, for example `gains.kP = 0.8`
(also `kI`, `kD`, `kS`, `kV`, `kA`; `pidConstants` and `ffCoefficients` are `internal` and can't be used from our
code).

## 5. Bring-up and tuning, in order

Do each step on blocks or with the robot held, then on the floor. Do not skip ahead: later steps depend on earlier
values.

### 5.1 Motor directions and gearing — *Motor test*

Plain SDK OpMode: drives one motor from the gamepad 1 left stick; nothing else runs. Set `MotorTest.expansionHub`
and `MotorTest.port` in Panels **before pressing INIT**. Telemetry shows power, position (counts) and velocity
(counts/s).

1. **Intake** (Control Hub, 2): stick up must pull balls in. If not, set `Intake.power` to -1.0.
2. **Transfer** (Control Hub, 3): stick up must move balls toward the flywheel. If not, make `Transfer.power` and
   `Transfer.feedPower` negative.
3. **Turret** (Expansion Hub, 2), cables slack, small stick movements:
   - Stick up must turn the turret **clockwise seen from above** and the position must **increase**. If it turns
     counter-clockwise, the motor needs `direction = REVERSE` (code). If position decreases while power is positive,
     stop: motor and encoder disagree, `REVERSE` won't fix it and the PID cannot work. Check the encoder cable.
   - Gearing: mark the start, turn exactly 90° with the stick, read the change in counts.
     `RADIANS_PER_COUNT = (π / 2) / counts`. Equivalently 2π / (28 × gearbox ratio × turret gear ratio).
   - Measure the real safe travel from the reference in both directions (expected about 500° one way) for 5.3.
4. **Flywheel** (Expansion Hub, 3): stick up must spin it in the launch direction **and** velocity must be
   positive. Otherwise it needs `direction = REVERSE` (code). Note the top velocity at full stick:
   RPM = counts/s × 60 / 28. If the motor is not a bare 6000 RPM motor, `MAX_RPM` must change (code).

### 5.2 Servo positions — *Servo test*

Plain SDK OpMode: dpad up/down step one servo by `ServoTest.step` (0.03). Set `ServoTest.expansionHub`, `port` and
`step` in Panels before INIT. **At START the servo jumps to 0.5**: make sure 0.5 is safe for that linkage, or
unhook it first. The servo goes limp at STOP.

| Servo | Hub, port | Find |
|---|---|---|
| Gate | Control Hub, 5 | `OPEN_POS` (ball passes in), `HOLD_POS` (ball held still in front of the sensor), `CLOSE_POS` (ball pushed into the transfer) |
| Guard | Expansion Hub, 2 | confirm 1.0 = closed and 0.0 = open; if not, change `Guard.CLOSED` / `OPEN` (code) |
| Hood | Expansion Hub, 0 | the range without hard stops. The code only clamps to 0..1, so if the safe range is narrower, the clamp in `Shooter.hood` must change (code) |
| Rail | Expansion Hub, 1 | `railPollen` and `railNectar` (rail spacing for each ball) |

The indicator light (Control Hub, 4) is not a positional servo; skip it.

### 5.3 Turret gearing and limits (code)

1. Put the measured `RADIANS_PER_COUNT` into `Turret.kt`, deploy.
2. Leave `MIN_RAD` / `MAX_RAD` at ±60° (or narrower) for PID tuning.
3. Only after the PID works (5.5): set the measured travel, minus a safety margin each side. `MIN_RAD` = the
   counter-clockwise travel from the reference (negative, probably close to 0), `MAX_RAD` = the clockwise travel,
   both in radians. The code then unwinds automatically when the HIVE moves out of
   reach.

### 5.4 Collecting — *Gate test*, then *Full test* or an autonomous

*Gate test* (NextFTC, so the transfer spins from START): cross = gate open, square = close, triangle = hold,
circle = open, wait for a yellow ball within 160 mm, hold, close. Telemetry: `Gate position`, `Gate sensor output`
(raw color/distance string).

1. Check the three gate positions with a ball by hand.
2. **Distance**: hold a ball where it rests against the gate; read the distance in `Gate sensor output`. Set
   `BALL_MM` a little above it, and check that with no ball the distance stays well above `BALL_MM`.
3. **Gain**: with the brightest ball held, raise `COLOR_GAIN` until its largest channel reads about 200 of 255. Too
   low and hue is garbage; don't let any channel reach 255.
4. **Hues**: hold each ball type under competition-like lighting and note hue and saturation. Edit
   `Gate.RED` / `YELLOW` / `BLUE` (and the tolerance) in `Gate.kt` to match; these are code, not Panels.
   Hue tolerance is ±25°, so profiles must stay more than 50° apart.
5. Run real collection (square in *Full test*, once Pedro works; see 5.8). Don't INIT an autonomous first: it counts
   4 preloads, and collecting stops at 4. Watch the indicator. Every yellow ball and
   your alliance's NECTAR must be kept (blue light, then the ball goes into the transfer), the other NECTAR
   rejected (fast red). Tune `Intake.rejectS` until a rejected ball fully leaves, `Gate.transferS` if a kept ball
   doesn't make it into the transfer before the gate reopens, `colorTimeoutS` short enough not to stall.
6. Repeated control of the opponent's NECTAR is a penalty (G408) even with auto-reject: rejection must be quick.

### 5.5 Turret PID — *Turret test*

Controls: dpad left/right = turret target ∓10°; dpad up/down = RPM ±100; bumpers = hood ±0.05; right trigger =
switch POLLEN/NECTAR; circle = toggle HIVE aiming (needs Pedro); cross = fire one ball. The turret target is set to
0° at INIT. Works without Pedro.

1. Prerequisite: 5.3 step 1 done. Turret at the reference at power-up.
2. In Panels, graph `Turret deg` and `Turret target deg`.
3. Under `Turret.gains.pidConstants`, set a small `kP`; step with dpad. Raise `kP` until it reaches the target
   quickly with a small overshoot.
4. Add `kD` to damp overshoot. If it stops short of the target, add a little `kS` (in `ffCoefficients`); use `kI`
   only if a steady error remains.
5. It must settle within `alignedRad` (2°) at every step, otherwise shots never fire.
6. Commit the gains (section 4, "Committing gains").

### 5.6 Flywheel PID — *Turret test*

1. Graph `Shooter rpm` and `Shooter target rpm`. Raise the target with dpad up.
2. With `Shooter.gains` all 0, raise `ffCoefficients.kV` until measured RPM is close to the target at several
   speeds; add `kS` if low speeds lag.
3. Add `pidConstants.kP` to remove the rest of the error and to recover fast after a shot. Too much kP oscillates.
4. It must hold within `rpmTolerance` (100). Commit the gains.

### 5.7 Single-ball feeding — *Turret test*

Put balls in the transfer by hand (it runs from START) and set an RPM that throws a ball safely. Cross fires one
ball as soon as telemetry `Ready to fire` is true (flywheel at speed and turret aligned).

1. Watch `Shooter rpm` during a shot and note the dip. Set `shotDipRpm` to roughly half of it, above normal noise.
   Too high: no shot is detected, and in a match the guard keeps reopening. Too low: noise counts as a shot.
2. `Guard.maxOpenS`: a bit longer than the time from guard opening to the dip.
3. `Transfer.feedPower`: lower it until exactly one ball passes per opening. If two balls pass at any setting, it is a
   mechanical problem (the guard closes too slowly); report it rather than hiding it in software.

### 5.8 Pedro, start pose, HIVE and Limelight

**Pedro (required for HIVE aiming, Full test and launch tables).** `pedro/Constants.java` `create()` returns
`null`, so *Full test* crashes at INIT and *Turret test* cannot aim at the HIVE. Set up and tune Pedro (mecanum,
Pinpoint) following the [Pedro docs](https://pedropathing.com/docs) and
[Pinpoint tuning](https://pedropathing.com/docs/pathing/tuning/localization/pinpoint); fill `Constants.create` with
the result. The tuners are in `pedro/procedures`, but `pedro/Tuning.java` is empty, so they aren't launchable yet.

**Start pose.** The robot starts every match at the same pose, not yet measured. Pedro frame (assumed, not
confirmed): origin at the audience-side corner on red's side, +x toward blue, +y away from the audience, inches and
radians counter-clockwise. Blue uses the same numbers (the field is symmetric under a 180° turn). *Turret test* takes
it from `TurretTest.startX/startY/startHeadingRad` in Panels (set before START every time).

**Turret axis.** Measure how far the turret's rotation axis sits to the side of the Pinpoint origin: `AXIS_Y_IN`,
inches, + = left, − = right. Until set it is NaN and HIVE aiming holds still (telemetry says so). The code also
assumes the axis is level with the Pinpoint origin front-to-back and the reference points straight ahead; if either is
false, the aiming math needs a code change.

**Limelight.**
1. Plug the Limelight into a blue USB 3.0 port on the Control Hub. In the Driver Station, open Configure Robot and
   scan: an "Ethernet Device" appears. Name it `limelight` and save the configuration.
2. Set up the pipeline over Wi-Fi: with the robot app running and the laptop on the Control Hub's Wi-Fi, open
   http://192.168.43.1:5801. The Panels Limelight proxy forwards it to the camera (stream on port 5800). Wait about
   20 s after power-up for the camera's green light. Pick pipeline **0** in the pipeline selector. Then:
   - Input tab: Pipeline Type = Fiducial Markers, Black Level = 0, resolution 640x480 (the code only needs 2D), Gain
     around 15, exposure as low as still shows the tags clearly (less motion blur).
   - Standard tab: Family = AprilTag Classic 36h11, Marker Size = 82.55 mm (3.25 in tags). Leave ID Filters empty
     or set them to 30–45. The code uses the IDs of both alliances (30–45), so don't filter to one ID.
   - Check that HIVE tags show up in the stream. Reconnect once to confirm the settings were kept.

   Details: [Limelight 3A setup](https://docs.limelightvision.io/docs/docs-limelight/getting-started/limelight-3a),
   [AprilTag pipelines](https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltags).
3. *Limelight test* (no controls) shows `Hive state`: AUDIENCE, COUNTER or NA. Alliance comes from the last
   autonomous INIT (RED by default; run the Blue autonomous INIT first to test blue).
4. Tilt a real HIVE each way. `AUDIENCE` must mean the audience-side CELL is up. If it is the opposite, swap
   `Robot.AUDIENCE_HIVE` and `COUNTER_HIVE` (code).
5. With no tags visible it must show NA, and the robot must not fire.

### 5.9 Launch tables — *Turret test* (needs Pedro, start pose, `AXIS_Y_IN`, Limelight seeing the HIVE)

The HIVE aim point comes from `Hive state`; while it is NA the turret holds still and the distance is NaN.


1. Enter the start pose in Panels during INIT, place the robot there, START.
2. Push the robot to a shooting spot. Circle: turret aims at the HIVE. Telemetry `Hive distance (in)` shows the
   distance.
3. Right trigger picks the ball type (`Rail ball` in telemetry). Set RPM (dpad up/down) and hood (bumpers), cross to
   fire. Repeat until it scores reliably.
4. Write the row: `Shot(Hive distance (in), Shooter target rpm, Hood position)` into `POLLEN_SHOTS` or
   `NECTAR_SHOTS` in `Shooter.kt`.
5. Cover the whole shooting area, about every 12 in. Distances must be strictly increasing; an unsorted table
   blocks firing. Between rows values are interpolated; beyond the ends the end row is used.

### 5.10 Full match behavior — *Full test*

Controls: left stick drives (field-centric: stick up = toward +x, away from the drivers), right stick turns, square
toggles collecting, dpad up/down RPM, bumpers hood, right trigger ball type (a running shot overwrites RPM and hood).
The turret aims at the HIVE from START. One shot is queued at START for every ball already counted.

1. INIT the Red or Blue autonomous first (INIT is enough): it sets the alliance and counts the 4 preloaded POLLEN.
2. *Full test* has no start pose: the direction the robot faces at START becomes +x.
3. Expect: preloads fire automatically, then square collects up to 4, each kept ball is fired automatically.
4. `leadS`: turn in place and drive past the HIVE; raise it until the turret holds its bearing while moving.

## 6. OpMode reference

| OpMode | Type | What it is for | Needs |
|---|---|---|---|
| Motor test | Utility, plain SDK | one motor by port, direction and encoder | hub/port in Panels before INIT |
| Servo test | Utility, plain SDK | one servo by port, find positions | hub/port/step in Panels before INIT |
| Gate test | Utility | gate positions and color/distance sensor | – |
| Limelight test | Utility | HIVE state from AprilTags | Limelight configured |
| Turret test | Utility | turret PID, flywheel PID, feeding, launch tables | Pedro, start pose and Limelight only for HIVE aiming |
| Full test | Utility | match-like TeleOp | Pedro (crashes at INIT without it) |
| Red / Blue | Autonomous | **placeholders**: only set alliance and count 4 preloads | – |

Not built yet: real autonomous (driving, shooting preloads, handing pose and turret reference to TeleOp), the match
TeleOp, a drivetrain mechanism, Limelight aim correction, manual setting/clearing of the ball count. See "Final
robot checklist" in `docs/MECHANISMS.md`.

## 7. Debugging

Telemetry keys, by mechanism: `Alliance`, `Balls` (count and order, first = next shot), `Robot state`,
`Hive state`, `Gate position`, `Gate sensor output`, `Intake throttle`, `Transfer throttle`, `Guard position`,
`Turret deg`, `Turret target deg`, `Shooter rpm`, `Shooter target rpm`, `Hood position`, `Rail ball`; in the test
OpModes also `Ready to fire`, `Hive distance (in)`, pose `x (in)`, `y (in)`, `heading (deg)`.

| Symptom | Check |
|---|---|
| Full test crashes at INIT | Pedro not configured (`Constants.create` returns null) |
| A device does nothing / wrong device moves | `Ports.kt` vs wiring; Control Hub servo ports 0–3 are dead; device also in the Robot Configuration on that port |
| Turret spins and doesn't stop | `RADIANS_PER_COUNT` still 0, wrong motor direction, or encoder unplugged. STOP, power off, return turret to reference |
| Turret angle is off after a while | turret moved by hand while powered, or not at the reference at power-up; power-cycle at the reference |
| Turret doesn't aim at the HIVE | `AXIS_Y_IN` NaN (telemetry says so), `Hive state` NA, no start pose (Turret test), Pedro missing |
| Turret aims at the wrong CELL | swap `AUDIENCE_HIVE` / `COUNTER_HIVE`; or the Pedro frame assumption is wrong |
| Never fires (`Balls` not empty) | in order: launch table empty or unsorted; `Hive state` NA (orange blink); `AXIS_Y_IN` NaN (`Hive distance` NaN); RPM never within `rpmTolerance` (flywheel gains); turret never within `alignedRad` (turret gains) |
| Guard opens and closes repeatedly, `Balls` doesn't shrink | `shotDipRpm` too high, or `maxOpenS` too short |
| Two balls per shot | lower `feedPower`; else mechanical (guard closes too slowly) |
| Every ball rejected / unknown | `COLOR_GAIN`, hue profiles, `BALL_MM`; read `Gate sensor output` |
| Square does nothing in Full test | already 4 balls counted (green solid); preloads are counted at autonomous INIT |
| `Balls` wrong after redeploy or restart | ball count resets to empty on Sloth deploy/restart; INIT an autonomous to recount preloads |
| Alliance wrong | defaults to RED; INIT the right autonomous first |
| Panels value reverted | Panels edits don't survive deploy/restart; commit them in code |
| Indicator dark | normal during INIT; otherwise check servo port 4 wiring |
