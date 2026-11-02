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

Not declared: `com.pedropathing.ivy:pedro` (Pedro's Ivy commands).

## Build

- Gradle daemon toolchain: JDK 25 (`gradle/gradle-daemon-jvm.properties`). No system `java`; Gradle-provisioned
  JDKs live in `~/.gradle/jdks/`.
- All sources (Kotlin and Java) live in `src/main/java`. javac only compiles `src/main/java`; Java files placed
  under `src/main/kotlin` were skipped (`compileDebugJavaWithJavac` reported `NO-SOURCE`) and their classes were
  missing from the APK, while Kotlin still compiled against them.

## NextFTC v2 0.3.0 (robot-0.3.0-sources.jar)

- `Mechanism` (`Mechanism.kt`): `periodic()`, `defaultCommand` (default `infinite {}`), helpers `instant`,
  `infinite`, `coroutine` that require the mechanism. No end/stop hook.
- `NextRobot` (`NextRobot.kt`): `periodic()`, `mechanisms: Set<Mechanism>`.
- Robot discovery (`RobotScanner.kt`): exactly one public `NextRobot` class, `object` or public no-arg
  constructor. Only runs under the Sloth class loader. The instance is created once at scan time and shared by
  every OpMode until the next Sloth reload.
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
- `Telemetry` (`Telemetry.kt`): `log(key, String)`, `log(key, Any?)`, `log(line)`; reaches the Driver Station.

## NextFTC hardware 0.3.0 (hardware-0.3.0-sources.jar)

- `NextLimelight(name)` (`webcams/NextLimelight.kt`): `startReading(pipeline, hz = 100)`, `start()`, `stop()`,
  `setPipeline`, `setPollRate`, `latestResult`, `isRunning`, `isConnected`, `tX`, `tY`, `getDistance`,
  `getPose()`, `getPedroPose()`. `latestResult` is declared non-null over the SDK getter; `getPose()` itself
  null-checks the SDK result.

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
