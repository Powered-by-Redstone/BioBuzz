# Hardware

Every device the code uses. Confirmed = provided by the team; Pending = not yet confirmed. Hub ports live in
`Ports.kt` (`ControlHub`, `ExpansionHub`); Robot Configuration names are written in the owning mechanism's file.

| Device | Hub / port or config name | Detail | Status | Code |
|---|---|---|---|---|
| Intake motor | Control Hub, motor port 2 | Addressed by port, no Robot Configuration name | Confirmed | `ControlHub.INTAKE_MOTOR` |
| Intake motor direction | | FORWARD assumed to pull balls in | Pending (test on robot) | `Intake` (NextMotor default) |
| Transfer motor | Control Hub, motor port 3 | Port chosen in code (2026-11-04); wire to match | Pending (wire to port 3) | `ControlHub.TRANSFER_MOTOR` |
| Transfer motor direction | | FORWARD assumed to push balls toward the feeder | Pending (test on robot) | `Transfer` (NextMotor default) |
| Turret motor | Expansion Hub, motor port 2 | REV UltraPlanetary, motor encoder | Confirmed | `ExpansionHub.TURRET_MOTOR` |
| Turret gearing | | Encoder counts per turret radian unknown | Pending | `Turret.RADIANS_PER_COUNT` (0 = encoder reads 0) |
| Turret direction | | FORWARD assumed to turn clockwise for positive degrees | Pending (test on robot) | `Turret` (NextMotor default) |
| Flywheel motor | Expansion Hub, motor port 3 | Velocity from the motor's own encoder (28 counts/motor rev) | Confirmed | `ExpansionHub.SHOOTER_MOTOR` |
| Flywheel gearbox / motor model | | Bare-motor 28 CPR and 6000 RPM free speed assumed | Pending | `Shooter.RADIANS_PER_COUNT`, `MAX_RPM` |
| Hood servo | Expansion Hub, servo port 0 | Position 0..1 | Confirmed | `ExpansionHub.HOOD_SERVO` |
| Hood servo usable range | | Full 0..1 assumed safe | Pending (check for hard stops) | `Shooter.hood` |
| Rail servo | Expansion Hub, servo port 1 | Port chosen in code (2026-11-11); wire to match | Pending (wire to port 1) | `ExpansionHub.RAIL_SERVO` |
| Rail positions | | POLLEN and NECTAR both 0 until measured | Pending | `Shooter.railPollen`, `railNectar` |
| Guard servo | Expansion Hub, servo port 2 | 1 = closed (balls wait), 0 = open (team, 2026-11-12) | Confirmed | `ExpansionHub.GUARD_SERVO`, `Guard.CLOSED/OPEN` |
| Limelight 3A | config name `"limelight"` | Pipeline 0 | Pending | `Limelight` |
| goBILDA RGB Indicator Light (3118-0808-0002) | Control Hub, servo port 4 | Moved off broken port 0 (2026-11-11); wire to match | Pending (wire to port 4) | `ControlHub.INDICATOR_SERVO` |
| Gate servo | Control Hub, servo port 5 | Position 0..1; moved off port 4, which the Indicator uses (2026-11-12); wire to match | Pending (wire to port 5) | `ControlHub.GATE_SERVO` |
| Gate color/distance sensor | Control Hub, I2C bus 2 | REV Color Sensor V2; addressed by bus, no Robot Configuration name | Confirmed | `ControlHub.GATE_COLOR_SENSOR_I2C_BUS` |

Control Hub servo ports 0-3 are broken (reported 2026-11-11); use Control Hub ports 4-5 or Expansion Hub ports.

Port-addressed devices are not checked against the Robot Configuration. If a named device is also configured on a port
the code addresses (for example Control Hub motor port 2 or I2C bus 2), both drive the same device and NextFTC does not
detect the conflict.
