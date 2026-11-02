# Hardware

Every device the code uses. Confirmed = provided by the team; Pending = not yet confirmed.

| Device | Hub / port or config name | Detail | Status | Code |
|---|---|---|---|---|
| Intake motor | Control Hub, motor port 2 | Addressed by port, no Robot Configuration name | Confirmed | `Intake.MOTOR_PORT` |
| Intake motor direction | | FORWARD assumed to pull balls in | Pending (test on robot) | `Intake` (NextMotor default) |
| Limelight 3A | config name `"limelight"` | Pipeline 0 | Pending | `LLTest` (hardcoded) |

Port-addressed devices are not checked against the Robot Configuration. If a named device is also configured on Control
Hub motor port 2, both write to the same motor and NextFTC does not detect the conflict.
