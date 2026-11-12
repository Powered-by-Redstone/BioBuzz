# Library notes

Verified facts only. Installed code wins over documentation. Sources are Gradle cache artifacts under
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
- All sources (Kotlin and Java) live in `src/main/java`. javac only compiles `src/main/java`; Java files placed
  under `src/main/kotlin` were skipped (`compileDebugJavaWithJavac` reported `NO-SOURCE`) and their classes were
  missing from the APK, while Kotlin still compiled against them.

- Unit tests: `src/test/java`, run with `./gradlew --offline testDebugUnitTest` (plain JVM, no robot needed).

## NextFTC v2 0.3.0 (robot-0.3.0-sources.jar)

- API reference: https://kdoc.nextftc.dev/0.3.0/ (control, linalg, units, hardware, robot). The page header reads
  `0.3.0-SNAPSHOT` (checked 2026-11-04); the installed sources stay authoritative. `/release/` and `/snapshot/`
  track the latest versions and will drift from the pinned one.

- `Mechanism` (`Mechanism.kt`): `periodic()`, `defaultCommand` (default `infinite {}`), helpers `instant`,
  `infinite`, `coroutine` that require the mechanism. No end/stop hook. `instant(action)` is
  `Commands.instant(action).requiring(this)`; both are priority 0, so a mechanism `instant` interrupts that
  mechanism's running non-default command (conflict `OVERRIDE`). Use `Commands.instant` (no requirements) for
  commands that only change a setpoint.
- `NextRobot` (`NextRobot.kt`): `periodic()`, `mechanisms: Set<Mechanism>`.
- Robot discovery (`RobotScanner.kt`): exactly one public `NextRobot` class, `object` or public no-arg
  constructor. Only runs under the Sloth class loader. The instance is created once at scan time and shared by
  every OpMode until the next Sloth reload.
- Docs (nextftc.dev/robot/nextrobot, /nextopmode, /mechanisms, checked 2026-11-03): exactly one `NextRobot`, and
  every OpMode gets the same mechanisms set. There is no documented way to run a subset of mechanisms in one OpMode;
  a mechanism stays passive unless its default command or an OpMode makes it act.
- An OpMode can't pass its own small robot: Kotlin compiles `object : NextRobot {}` as a public class (anonymous
  classes get ACC_PUBLIC, checked with javap), so the scanner would find a second robot. `MotorTest` is therefore a
  plain SDK `LinearOpMode` (SDK 12 `@Utility(name, description)`) that drives `DcMotorImplEx(hub.motorController, port)` (`NextLynxModule.motorController`
  is public); no mechanisms run, so it can drive ports the mechanisms own.
- OpMode discovery (`NextFTCOpModeScanner.kt`): public constructor with one parameter whose type is a supertype of
  the robot class, or no-arg. Annotations: `@NextTeleop(name, group)`, `@NextAutonomous(name, group,
  preselectTeleop)`, `@NextUtility(name, description)`.
- `NextOpMode(robot, vararg hooks)` (`NextOpMode.kt`): exposes `gamepad1`, `gamepad2`, `telemetry`,
  `hardwareMap` as fields, usable in subclass property initializers. Overridables: `disabledPeriodic()`,
  `start()`, `periodic()`, `end()`. `end()` is skipped if the loop throws.
- Hook order: user hooks first, then Robot, Scheduler, Motor, Telemetry (`OpModeHook.kt`).
- Per-loop order while running:
  1. `opMode.periodic()`
  2. `BulkReadHook` clears the bulk cache (also once after start)
  3. `robot.periodic()`, then each `mechanism.periodic()`
  4. `Trigger.defaultEventLoop.poll()`, then `Scheduler.execute()` (one Ivy scheduler)
  5. `NextMotor.motorEventLoop.poll()`
  6. `Telemetry.update()`
- INIT loop: only `disabledPeriodic()` and `Telemetry.update()`. No robot/mechanism periodic, no scheduler.
  Default commands are scheduled in `beforeStart`.
- `Telemetry` (`Telemetry.kt`): `log(key, String)`, `log(key, Any?)`, `log(line)`. Default backend is the Driver
  Station only; `addBackend(sdkTelemetry)` adds more (a set, so duplicates are ignored). `Robot` adds
  `PanelsTelemetry.ftcTelemetry` (Panels does not mirror Driver Station telemetry on its own). Panels web UI:
  port 8001 on the robot (panels-0.3.2+1.0.5 `StaticServer(context, 8001, ...)`).

- Gamepad triggers (`triggers/CommandGamepad.kt`, `triggers/Trigger.kt`): `CommandGamepad(gamepad)` exposes
  `Trigger`s such as `square`, `cross`, `x`, bumpers, d-pad. `onTrue(cmd)` schedules on the rising edge;
  also `onFalse`, `whileTrue`, `toggleOnTrue`. Bindings go on `Trigger.defaultEventLoop`, polled only while the
  OpMode runs (not in INIT) and cleared when it ends.

## NextFTC hardware 0.3.0 (hardware-0.3.0-sources.jar)

- `NextMotor(module: NextLynxModule, port: Int)` (`actuators/NextMotor.kt`): addresses a motor by hub port
  (0..3), no Robot Configuration name. `RobotController.controlHub` / `expansionHub` give the hubs. A port used
  twice through this API throws "Port conflict"; named devices are not checked.
- `NextMotor.throttle = x` sets `controlType = ControlType.Throttle(x)`, which throws unless x is in -1.0..1.0.
  Power is written in `MotorHook` each loop. On OpMode stop the motor resets to `Throttle(0.0)`.
- Position control: `NextMotor(module, port, anglePerCount)`; `setPositionSetpoint(Angle)` sets
  `ControlType.Position`. Output = PID(setpoint, `encoderPosition`) + `kS·sign(error)` + `kG` + `kCos` term, from
  `positionConstants: MotorPositionConstants` (a `val` created by NextMotor; fields `pidConstants` (kP, kI, kD),
  `ffCoefficients` (kS, kV, kA), `kG`, `kCos`, `kCosRatio`). `positionPID` shares the same `pidConstants` object.
  No encoder reset API. SDK `LynxDcMotorController` clips power to ±1, so large PID output does not throw.
- Velocity control: `setVelocitySetpoint(AngularVelocity)`; output = PID(setpoint, `encoderVelocity`) +
  `kS·sign + kV·v + kA·a` from `velocityConstants: MotorVelocityConstants` (`pidConstants`, `ffCoefficients`).
  `encoderVelocity` = `anglePerCount` × SDK `getVelocity()` (counts/s). The REV hub reports velocity as a
  16-bit `short` (`LynxGetBulkInputDataResponse.velocities`), max 32,767 counts/s.
- `NextServo(module, port)` (`actuators/NextServo.kt`): servo ports 0..5; `position` (0..1) writes through a cache.
- `NextRGBIndicator(module, port)` (`actuators/NextRGBIndicator.kt`): `NextServo` subclass for the goBILDA RGB
  Indicator Light. `Color` enum with servo positions (OFF 0.0, RED 0.279 … VIOLET 0.722, WHITE 1.0),
  `setColor(Color)`, `setColor(pwm: Double)`, `off()`. `setBrightness(b)` does not dim: it sets the PWM range to
  500..2500·b µs, which shifts every color (b = 1: RED = 1058 µs = off, VIOLET = 1944 µs = white). Do not call it.
- `NextColor.rgb(r, g, b)` (`sensors/colors/NextColor.kt`) throws unless each value is in 0..255.
  REV Color Sensor V3 `red()`/`green()`/`blue()` are raw counts 0..65535 (Hardware-12.0.0-sources
  `BroadcomColorSensorImpl.updateColors`), so never pass them to `NextColor.rgb`. `getNormalizedColors()` is
  clipped to 0..1.
- `NextColorDistanceSensor(name, hasDistance)` (`sensors/NextColorDistanceSensor.kt`): `update()` reads
  normalized colors × 255 and distance into a cache; call it once per loop (0.3.1-SNAPSHOT binds it to the
  motor event loop instead, but replaces `Telemetry` with a new module; not adopted 2026-11-11). `color`, `hue`, `saturation`,
  `value`, `getDistance(unit)` read the cache (distance NaN before the first `update()`). `gain` is software gain.
  REV V3 normalizes raw counts by `colorSaturation = 65535` (`BroadcomColorSensor.Parameters`), so at gain 1 a
  ball reads only a few of 255. HSV is computed from the truncated integer RGB, so low readings give garbage
  hue/saturation. Setting `gain` writes a field only (no I2C), so setting it every loop is cheap.
- `ColorProfile(space, color, tolerance)` (`sensors/colors/ColorProfile.kt`): HSV match = hue distance (wraps at
  360°) and |Δs|, |Δv| each within tolerance. `color` and `tolerance` are `NextColor`s stored as RGB and converted
  back to HSV through integer RGB, so give the tolerance saturation > 0 (else its hue becomes 0) and value 1
  (= brightness ignored). HSV is cached at construction: changing a profile needs a new instance.
- FTC SDK default servo PWM range is 600–2400 µs (`PwmControl.PwmRange.defaultRange`, RobotCore-12.0.0-sources
  `PwmControl.java:107-111`).
- Units (units-0.3.0): `Double.degrees` / `Double.radians` / `Double.rpm`, units `Degrees`, `RotationsPerMinute`, `Measure.into(unit)`; imports from
  `dev.nextftc.units`.

- `NextLimelight(name)` (`webcams/NextLimelight.kt`): `startReading(pipeline, hz = 100)`, `start()`, `stop()`,
  `setPipeline`, `setPollRate`, `latestResult`, `isRunning`, `isConnected`, `tX`, `tY`, `getDistance`,
  `getPose()`, `getPedroPose()`. `latestResult` is declared non-null over the SDK getter; `getPose()` itself
  null-checks the SDK result.

## Ivy 1.1.1 (core-1.1.1.jar via javap)

- `CommandBuilder` implements `Command`. `Commands.instant/infinite/conditional/branch/lazy/match/waitMs/waitUntil`.
- Defaults (`CommandBuilder()`): priority 0, interrupted `END`, blocked `CANCEL`, conflict `OVERRIDE`.
- `Scheduler.schedule` (javap -c): if any required mechanism is held by a higher-priority command, the new one is
  blocked (`CANCEL` drops it, `QUEUE` queues it). Equal priority uses the conflict behavior (`OVERRIDE` interrupts
  the holder, `QUEUE` queues, `CANCEL` drops). Lower priority holders are interrupted (`END` ends them, `SUSPEND`
  parks them).
- `Scheduler.execute`: runs commands, then starts queued commands whose requirements are free, then resumes
  suspended ones in the order they were suspended. Resume ignores priority, so a mechanism's default command
  (suspended first) comes back before a lower-priority command that is still wanted.
- NextFTC wraps each default command (`Mechanism.kt` `forceDefaultCommand`): priority `Int.MIN_VALUE`, interrupted
  `SUSPEND`, conflict and blocked `QUEUE`.
- `Commands.conditional(cond, a, b)` picks `a` or `b` when it starts; requirements are the union of both,
  priority the max.

## Panels Configurables 0.3.2+1.0.5 (configurables-0.3.2+1.0.5-sources.jar)

- Class must carry `@com.bylazar.configurables.annotations.Configurable`. Only static, non-final fields are
  exposed (`ConfigurablesPlugin.scan`): in Kotlin, an `object` with `@JvmField var`, or a companion object's
  `@JvmField var` with `@Configurable` on the outer class (the static field lives on the outer class). Instance
  fields, `const val` and `val` are skipped. `@IgnoreConfigurable` excludes a class or field.
- Nested objects: a field of a non-`java.`/`kotlin.` class type is shown as a tree of its declared fields, built
  from the declared type at scan. Values are read and written through the static field at access time
  (`MyField.getValue/setValue`), so pointing the field at another instance after the scan works. Used to expose
  `NextMotor.positionConstants` (`Turret.gains`).

## Pedro 3.0.2 (core-3.0.2.jar via javap, revhub-3.0.2-sources.jar)

- `Follower(Localizer, Drivetrain, Algorithm)`. `update()` runs `localizer.update()` then drives according to
  `mode()` (`FOLLOW`, `HOLD`, `MANUAL`, `IDLE`). Call exactly once per loop.
- `manual(forward, strafe, turn)` / `manual(DrivePowers)`: robot-centric, sets mode `MANUAL`; powers are applied
  on the next `update()`. `stop()` only sets mode `IDLE`; motors stop on the next `update()`.
  `follower.drivetrain.stop()` stops them immediately.
- Sign convention (`Mecanum.computeWheelPowersUnnormalized`): `fl = f - s - t`, `fr = f + s + t`,
  `bl = f + s - t`, `br = f - s + t`. Positive strafe drives left, positive turn is counter-clockwise.
  Gamepad mapping: `forward = -left_stick_y`, `strafe = -left_stick_x`, `turn = -right_stick_x`.
- `ManualDrive.fieldCentric(forward, strafe, turn, heading[, offset])` rotates (forward, strafe) by
  `-(heading + offset)`; stick up drives toward field +x.
- `pose()` returns `Pose` with `x()`, `y()` (inches) and `heading()` (radians). Also `velocity()`, `twist()`.
  `velocity()` is `Velocity(vx, vy, omega)` (public final fields) from the localizer; `PinpointLocalizer.update()`
  fills it from `getVelX/getVelY` and `getHeadingVelocity(RADIANS)` (revhub-3.0.2-sources). `Pose.withHeading(h)`
  returns a copy. Frames (pedropathing.com/docs/pathing/reference/velocity, checked 2026-11-11): `Velocity` is
  world/field frame (same axes as poses), `Twist` is robot frame (x forward, y left); omega is the same in both.
  The SDK javadoc for Pinpoint `getVelX` only says "X (forward) velocity of the robot"; Pedro treats it as field
  frame.
