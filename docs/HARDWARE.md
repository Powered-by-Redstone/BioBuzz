# Hardware

Every device the code uses. Confirmed = provided by the team; Pending = not yet confirmed.

| Device | Hub / port or config name | Detail | Status | Code |
|---|---|---|---|---|
| Intake motor | Control Hub, motor port 2 | Addressed by port, no Robot Configuration name | Confirmed | `Intake.MOTOR_PORT` |
| Intake motor direction | | FORWARD assumed to pull balls in | Pending (test on robot) | `Intake` (NextMotor default) |
| Turret motor | Expansion Hub, motor port 2 | REV UltraPlanetary, motor encoder | Confirmed | `Turret.MOTOR_PORT` |
| Turret gearing | | Encoder counts per turret degree unknown | Pending | `Turret.DEGREES_PER_COUNT` (0 = encoder reads 0°) |
| Turret direction | | FORWARD assumed to turn clockwise for positive degrees | Pending (test on robot) | `Turret` (NextMotor default) |
| Flywheel motor | Expansion Hub, motor port 3 | Velocity from the motor's own encoder (28 counts/motor rev) | Confirmed | `Shooter.MOTOR_PORT` |
| Flywheel gearbox / motor model | | Bare-motor 28 CPR and 6000 RPM free speed assumed | Pending | `Shooter.DEGREES_PER_COUNT`, `MAX_RPM` |
| Hood servo | Expansion Hub, servo port 0 | Position 0..1 | Confirmed | `Shooter.HOOD_PORT` |
| Hood servo usable range | | Full 0..1 assumed safe | Pending (check for hard stops) | `Shooter.hood` |
| Limelight 3A | config name `"limelight"` | Pipeline 0 | Pending | `LLTest` (hardcoded) |

Port-addressed devices are not checked against the Robot Configuration. If a named device is also configured on Control
Hub motor port 2, both write to the same motor and NextFTC does not detect the conflict.
