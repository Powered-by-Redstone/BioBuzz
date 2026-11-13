# Library notes

Verified facts only. Installed code wins over docs. Sources: Gradle cache artifacts under
`~/.gradle/caches/modules-2/files-2.1/`.

## Pinned versions (build.gradle.kts, 2026-11-02)

| Library | Version | Declared via |
|---|---|---|
| `dev.frozenmilk.teamcode` plugin | 12.0.0-1.2.2 (FTC SDK 12.0.0) | plugins |
| `dev.frozenmilk.sinister.sloth.load` plugin | 0.3.2 | plugins |
| Sloth | 0.3.2 | `ftc { dairy }` |
| Panels `com.bylazar.sloth:fullpanels` (Sloth variant) | 0.3.2+1.0.13 | `ftc { dairy }` |
| NextFTC v2 `dev.nextftc.v2:control/hardware/robot` | 0.3.0 | `dependencies` |
| Ivy `com.pedropathing.ivy:core` | 1.1.1 | transitive (NextFTC robot) |
| Pedro `com.pedropathing:revhub` (+ `core`) | 3.0.2 | `dependencies` |
| Pedro `com.pedropathing:tuning` | 1.0.3 (asks for revhub 3.0.1, resolved to 3.0.2) | `dependencies` |
| JUnit `junit:junit` | 4.13.2 (unit tests only) | `testImplementation` |

Not declared: `com.pedropathing.ivy:pedro` (Pedro's Ivy commands).

## Build

- Gradle daemon toolchain: JDK 25 (`gradle/gradle-daemon-jvm.properties`). No system `java`; Gradle-provisioned
  JDKs live in `~/.gradle/jdks/`.
- All sources (Kotlin and Java) live in `src/main/java`. javac compiles only `src/main/java`; Java files under
  `src/main/kotlin` were skipped (`compileDebugJavaWithJavac` reported `NO-SOURCE`), classes
  missing from APK, while Kotlin still compiled against them.

- Unit tests: `src/test/java`, run with `./gradlew --offline testDebugUnitTest` (plain JVM, no robot needed).

## NextFTC v2 0.3.0 (robot-0.3.0-sources.jar)

- API reference: https://kdoc.nextftc.dev/0.3.0/ (control, linalg, units, hardware, robot). Page header reads
  `0.3.0-SNAPSHOT` (checked 2026-11-04); installed sources stay authoritative. `/release/` and `/snapshot/`
  track latest versions, will drift from pinned one.

- `Mechanism` (`Mechanism.kt`): `periodic()`, `defaultCommand` (default `infinite {}`), helpers `instant`,
  `infinite`, `coroutine` that require the mechanism. No end/stop hook. `instant(action)` is
  `Commands.instant(action).requiring(this)`; both priority 0, so mechanism `instant` interrupts that
  mechanism's running non-default command (conflict `OVERRIDE`). Use `Commands.instant` (no requirements) for
  commands that only change a setpoint.
- `NextRobot` (`NextRobot.kt`): `periodic()`, `mechanisms: Set<Mechanism>`.
- Coroutine commands (`coroutine/*.kt`): `command { }` and `Mechanism.coroutine { }` return
  `CoroutineCommandBuilder`; each `start()` builds fresh coroutine, so one instance can be rescheduled. Body helpers:
  `wait(s)`, `waitUntil`, `await(cmd)` (runs inline, its requirements not checked: put them on outer command),
  `awaitAny` (ends the others `INTERRUPTED`), `fork`. Interrupting resumes body with `CancellationException`, so
  `try/finally` runs; `return@command` ends body early.
- Robot discovery (`RobotScanner.kt`): exactly one public `NextRobot` class, `object` or public no-arg
  constructor. Runs only under Sloth class loader. Instance created once at scan time, shared by
  every OpMode until next Sloth reload.
- Docs (nextftc.dev/robot/nextrobot, /nextopmode, /mechanisms, checked 2026-11-03): exactly one `NextRobot`,
  every OpMode gets same mechanisms set. No documented way to run subset of mechanisms in one OpMode;
  mechanism stays passive unless its default command or an OpMode makes it act.
- OpMode can't pass its own small robot: Kotlin compiles `object : NextRobot {}` as public class (anonymous
  classes get ACC_PUBLIC, checked with javap), so scanner would find second robot. `MotorTest` is therefore
  plain SDK `LinearOpMode` (SDK 12 `@Utility(name, description)`) that drives `DcMotorImplEx(hub.motorController, port)` (`NextLynxModule.motorController`
  is public); no mechanisms run, so it can drive ports the mechanisms own.
- OpMode discovery (`NextFTCOpModeScanner.kt`): public constructor with one parameter whose type is supertype of
  robot class, or no-arg. Annotations: `@NextTeleop(name, group)`, `@NextAutonomous(name, group,
  preselectTeleop)`, `@NextUtility(name, description)`.
- `NextOpMode(robot, vararg hooks)` (`NextOpMode.kt`): exposes `gamepad1`, `gamepad2`, `telemetry`,
  `hardwareMap` as fields, usable in subclass property initializers. Overridables: `disabledPeriodic()`,
  `start()`, `periodic()`, `end()`. `end()` skipped if loop throws.
- Hook order: user hooks first, then Robot, Scheduler, Motor, Telemetry (`OpModeHook.kt`).
- Per-loop order while running:
  1. `opMode.periodic()`
  2. `BulkReadHook` clears bulk cache (also once after start)
  3. `robot.periodic()`, then each `mechanism.periodic()`
  4. `Trigger.defaultEventLoop.poll()`, then `Scheduler.execute()` (one Ivy scheduler)
  5. `NextMotor.motorEventLoop.poll()`
  6. `Telemetry.update()`
- INIT loop: only `disabledPeriodic()` and `Telemetry.update()`. No robot/mechanism periodic, no scheduler.
  Default commands scheduled in `beforeStart`, which reads each mechanism's `defaultCommand` property anew every
  OpMode (`OpModeHook.kt` `RobotHook`; only read in robot-0.3.0-sources).
- `Telemetry` (`Telemetry.kt`): `log(key, String)`, `log(key, Any?)`, `log(line)`. Default backend is Driver
  Station only; `addBackend(sdkTelemetry)` adds more (a set, duplicates ignored). `Robot` adds
  `PanelsTelemetry.ftcTelemetry` (Panels does not mirror Driver Station telemetry on its own). Panels web UI:
  port 8001 on robot (panels-0.3.2+1.0.5 `StaticServer(context, 8001, ...)`).

- Gamepad triggers (`triggers/CommandGamepad.kt`, `triggers/Trigger.kt`): `CommandGamepad(gamepad)` exposes
  `Trigger`s such as `square`, `cross`, `x`, bumpers, d-pad. `onTrue(cmd)` schedules on rising edge;
  also `onFalse`, `whileTrue`, `toggleOnTrue`. Bindings go on `Trigger.defaultEventLoop`, polled only while
  OpMode runs (not in INIT), cleared when it ends.

## NextFTC hardware 0.3.0 (hardware-0.3.0-sources.jar)

- `NextMotor(module: NextLynxModule, port: Int)` (`actuators/NextMotor.kt`): addresses motor by hub port
  (0..3), no Robot Configuration name. `RobotController.controlHub` / `expansionHub` give the hubs. Port used
  twice through this API throws "Port conflict"; named devices not checked.
- `NextMotor.throttle = x` sets `controlType = ControlType.Throttle(x)`, throws unless x in -1.0..1.0.
  Power written in `MotorHook` each loop. On OpMode stop motor resets to `Throttle(0.0)`.
- Position control: `NextMotor(module, port, anglePerCount)`; `setPositionSetpoint(Angle)` sets
  `ControlType.Position`. Output = PID(setpoint, `encoderPosition`) + `kS·sign(error)` + `kG` + `kCos` term, from
  `positionConstants: MotorPositionConstants` (a `val` created by NextMotor; fields `pidConstants` (kP, kI, kD),
  `ffCoefficients` (kS, kV, kA), `kG`, `kCos`, `kCosRatio`). `positionPID` shares same `pidConstants` object.
  No encoder reset API. SDK `LynxDcMotorController` clips power to ±1, so large PID output does not throw.
- Velocity control: `setVelocitySetpoint(AngularVelocity)`; output = PID(setpoint, `encoderVelocity`) +
  `kS·sign + kV·v + kA·a` from `velocityConstants: MotorVelocityConstants` (`pidConstants`, `ffCoefficients`).
  `encoderVelocity` = `anglePerCount` × SDK `getVelocity()` (counts/s). REV hub reports velocity as
  16-bit `short` (`LynxGetBulkInputDataResponse.velocities`), max 32,767 counts/s.
- `NextServo(module, port)` (`actuators/NextServo.kt`): servo ports 0..5; `position` (0..1) writes through cache.
- `NextFeedbackCRServo(module, port, feedback: AnalogFeedback)` (`actuators/NextFeedbackCRServo.kt`): `NextCRServo`
  (`power` -1..1, write-cached, cache reset on OpMode stop) plus `angle` = feedback voltage / `maxVoltage` × 2π, no
  unwrapping, so 0..2π per servo turn. `NextAnalogInput(module, channel)` (`sensors/NextAnalogInput.kt`): channel
  0..3, `maxVoltage` default 3.3 V. Axon docs (docs.axon-robotics.com/servos/analog-output, checked 2026-11-13) give no
  voltage span or continuous-mode behavior ("Docs coming soon"), so 3.3 V = 360° unverified.
- Control 0.3.0 (`control-0.3.0.jar`, javap; no sources jar): no PIDF class. `PIDController(PIDCoefficients(kP,
  kI, kD))` (public fields, held by reference, so Panels edits reach it), `calculate(reference =, measured =)`,
  `error`, `reset()`. Feedforward separate: `SimpleFeedforward(SimpleFFCoefficients(kS, kV, kA))`,
  `ArmFeedforward`, `ElevatorFeedforward`. `NextMotor.update()` adds them itself (position: PID + `kS·sign(error)` +
  `kG` + `kCos`; velocity: PID + `SimpleFeedforward`).
- `NextRGBIndicator(module, port)` (`actuators/NextRGBIndicator.kt`): `NextServo` subclass for goBILDA RGB
  Indicator Light. `Color` enum with servo positions (OFF 0.0, RED 0.279 … VIOLET 0.722, WHITE 1.0),
  `setColor(Color)`, `setColor(pwm: Double)`, `off()`. `setBrightness(b)` does not dim: sets PWM range to
  500..2500·b µs, which shifts every color (b = 1: RED = 1058 µs = off, VIOLET = 1944 µs = white). Do not call it.
- `NextColor.rgb(r, g, b)` (`sensors/colors/NextColor.kt`) throws unless each value in 0..255.
  REV color sensor `red()`/`green()`/`blue()` are raw counts (V2: up to 10240 at default 24 ms integration,
  Hardware-12.0.0-sources `AMSColorSensor.Parameters.getMaximumReading`), so never pass them to `NextColor.rgb`.
  `getNormalizedColors()` clipped to 0..1.
- `NextColorDistanceSensor(module, bus, hasDistance)` (`sensors/NextColorDistanceSensor.kt`): addresses sensor
  by hub I2C bus (0..3), no Robot Configuration name; claims the bus. Builds `LynxI2cColorRangeSensor`, the SDK's
  REV Color Sensor V2 driver (TMD37821 at 0x39); V3 (APDS-9151 at 0x52) needs `RevColorSensorV3` instead, so this
  constructor is V2 only. Robot's gate sensor is V2 (team, 2026-11-12). V2 `getNormalizedColors()` polls
  until data valid with `delay(3)` between reads (up to 2 s; returns zeros at once if disconnected).
- `NextColorDistanceSensor(name, hasDistance)` (same file): `update()` reads
  normalized colors × 255 and distance into cache; call it once per loop (0.3.1-SNAPSHOT binds it to
  motor event loop instead, but replaces `Telemetry` with new module; not adopted 2026-11-11). `color`, `hue`, `saturation`,
  `value`, `getDistance(unit)` read the cache (distance NaN before first `update()`). `gain` is software gain.
  V2 normalizes raw counts by `getMaximumReading()` (10240 by default), so at gain 1 a ball reads only a few
  of 255 (observed 2026-11-11). HSV computed from truncated integer RGB, so low readings give garbage
  hue/saturation. Setting `gain` writes a field only (no I2C), so setting it every loop is cheap.
- `ColorProfile(space, color, tolerance)` (`sensors/colors/ColorProfile.kt`): HSV match = hue distance (wraps at
  360°) and |Δs|, |Δv| each within tolerance. `color` and `tolerance` are `NextColor`s stored as RGB, converted
  back to HSV through integer RGB, so give tolerance saturation > 0 (else its hue becomes 0) and value 1
  (= brightness ignored). HSV cached at construction: changing a profile needs new instance.
- FTC SDK default servo PWM range is 600–2400 µs (`PwmControl.PwmRange.defaultRange`, RobotCore-12.0.0-sources
  `PwmControl.java:107-111`).
- Units (units-0.3.0): `Double.degrees` / `Double.radians` / `Double.rpm`, units `Degrees`, `RotationsPerMinute`, `Measure.into(unit)`; imports from
  `dev.nextftc.units`.

- `NextLimelight(name)` (`webcams/NextLimelight.kt`): `startReading(pipeline, hz = 100)`, `start()`, `stop()`,
  `setPipeline`, `setPollRate`, `latestResult`, `isRunning`, `isConnected`, `tX`, `tY`, `getDistance`,
  `getPose()`, `getPedroPose()`. `latestResult` declared non-null over SDK getter; `getPose()` itself
  null-checks SDK result.

## Ivy 1.1.1 (core-1.1.1.jar via javap)

- `CommandBuilder` implements `Command`. `Commands.instant/infinite/conditional/branch/lazy/match/waitMs/waitUntil`.
- Defaults (`CommandBuilder()`): priority 0, interrupted `END`, blocked `CANCEL`, conflict `OVERRIDE`.
- `Scheduler.schedule` (javap -c): if any required mechanism held by higher-priority command, new one is
  blocked (`CANCEL` drops it, `QUEUE` queues it). Equal priority uses conflict behavior (`OVERRIDE` interrupts
  holder, `QUEUE` queues, `CANCEL` drops). Lower priority holders interrupted (`END` ends them, `SUSPEND`
  parks them).
- Queue order (javap -c `Scheduler.execute`): `queuedCommands` is deque walked in insertion order, each start
  takes its requirements before next is checked, so commands with same requirements start FIFO.
  `Command.schedule()` = `Scheduler.schedule(this)`; scheduling from inside running command is safe (execute walks
  copy of running list).
- `Scheduler.execute`: runs commands, then starts queued commands whose requirements are free, then resumes
  suspended ones in order suspended. Resume ignores priority, so a mechanism's default command
  (suspended first) comes back before a lower-priority command still wanted.
- NextFTC wraps each default command (`Mechanism.kt` `forceDefaultCommand`): priority `Int.MIN_VALUE`, interrupted
  `SUSPEND`, conflict and blocked `QUEUE`.
- `Commands.conditional(cond, a, b)` picks `a` or `b` when it starts; requirements are union of both,
  priority the max.

## Panels Configurables 0.3.2+1.0.5 (configurables-0.3.2+1.0.5-sources.jar)

- Class must carry `@com.bylazar.configurables.annotations.Configurable`. Only static, non-final fields
  exposed (`ConfigurablesPlugin.scan`): in Kotlin, an `object` with `@JvmField var`, or companion object's
  `@JvmField var` with `@Configurable` on outer class (static field lives on outer class). Instance
  fields, `const val` and `val` skipped. `@IgnoreConfigurable` excludes a class or field.
- Nested objects: field of non-`java.`/`kotlin.` class type shown as tree of its declared fields, built
  from declared type at scan. Values read and written through static field at access time
  (`MyField.getValue/setValue`), so pointing field at another instance after scan works. Used to expose
  `NextMotor.positionConstants` (`Turret.gains`).

## Game manual (BIOBUZZ Competition Manual TU06, section 9, read 2026-11-12)

- FIELD 144 in square, 36 tiles of 24 in. Red ALLIANCE AREA on left as seen from audience; layout symmetric
  under 180° turn. HIVE geometry and AprilTag IDs: see `docs/MECHANISMS.md` HIVE.
- G407 (section 11, TU08): ROBOT may not CONTROL more than 4 SCORING ELEMENTS at once; MOMENTARY control of 5
  reversed quickly is not STRATEGIC, REPEATED control of 5 or more is, even with a "keep 4, discard the
  rest" mechanism. Elements LAUNCHED and no longer touching ROBOT are not CONTROLLED. `Robot.MAX_BALLS`.
- G408: ROBOT may not CONTROL opponent NECTAR. REPEATED instances likely STRATEGIC (YELLOW CARD) even with
  mechanism that discards it automatically; so `collect`'s reject keeps it legal only if rare.

## Pedro 3.0.2 (core-3.0.2.jar via javap, revhub-3.0.2-sources.jar)
- Field frame (pedropathing.com/docs/pathing/reference/coordinates, 2026-11-12): origin bottom-left corner, +x right,
  +y up, heading radians counter-clockwise from +x; FTC SDK pose to Pedro: (y + 72, 72 - x, heading - π/2). Docs
  don't say which wall is audience. No `Pose.mirror()` in core-3.0.2.

- `Follower(Localizer, Drivetrain, Algorithm)`. `update()` runs `localizer.update()` then drives according to
  `mode()` (`FOLLOW`, `HOLD`, `MANUAL`, `IDLE`). Call exactly once per loop.
- `manual(forward, strafe, turn)` / `manual(DrivePowers)`: robot-centric, sets mode `MANUAL`; powers applied
  on next `update()`. `stop()` only sets mode `IDLE`; motors stop on next `update()`.
  `follower.drivetrain.stop()` stops them immediately.
- Sign convention (`Mecanum.computeWheelPowersUnnormalized`): `fl = f - s - t`, `fr = f + s + t`,
  `bl = f + s - t`, `br = f - s + t`. Positive strafe drives left, positive turn is counter-clockwise.
  Gamepad mapping: `forward = -left_stick_y`, `strafe = -left_stick_x`, `turn = -right_stick_x`.
- `ManualDrive.fieldCentric(forward, strafe, turn, heading[, offset])` rotates (forward, strafe) by
  `-(heading + offset)`; stick up drives toward field +x.
- `setPose(Pose)` sets current pose; no `setStartingPose` in core-3.0.2 (javap of
  `com.pedropathing.follower.Follower`, 2026-11-12).
- `pose()` returns `Pose` with `x()`, `y()` (inches) and `heading()` (radians). `Pose.distance(other)` (inches). Also `velocity()`, `twist()`.
  `velocity()` is `Velocity(vx, vy, omega)` (public final fields) from localizer; `PinpointLocalizer.update()`
  fills it from `getVelX/getVelY` and `getHeadingVelocity(RADIANS)` (revhub-3.0.2-sources). `Pose.withHeading(h)`
  returns a copy. Frames (pedropathing.com/docs/pathing/reference/velocity, checked 2026-11-11): `Velocity` is
  world/field frame (same axes as poses), `Twist` is robot frame (x forward, y left); omega same in both.
  SDK javadoc for Pinpoint `getVelX` only says "X (forward) velocity of the robot"; Pedro treats it as field
  frame.
