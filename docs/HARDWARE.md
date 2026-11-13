# Hardware

Every device code uses. Confirmed = provided by team; Pending = not yet confirmed. Hub ports live in
`Ports.kt` (`ControlHub`, `ExpansionHub`); Robot Configuration names written in owning mechanism's file.

| Device | Hub / port or config name | Detail | Status | Code |
|---|---|---|---|---|
| Intake motor | Control Hub, motor port 2 | Addressed by port, no Robot Configuration name | Pending (wire and check) | `ControlHub.INTAKE_MOTOR` |
| Intake motor direction | | FORWARD pulls balls in | Confirmed (team commit b126a0f, 2026-11-13) | `Intake` (NextMotor default) |
| Transfer motor | Expansion Hub, motor port 2 | Set by team (2026-11-13) | Pending (wire and check) | `ExpansionHub.TRANSFER_MOTOR` |
| Transfer motor direction | | FORWARD pushes balls toward feeder | Confirmed (team commit b126a0f, 2026-11-13) | `Transfer` (NextMotor default) |
| Turret motor | Expansion Hub, motor port 3 | REV UltraPlanetary, motor encoder; set by team (2026-11-13) | Pending (wire and check) | `ExpansionHub.TURRET_MOTOR` |
| Turret gearing | | Encoder counts per turret radian unknown | Pending | `Turret.RADIANS_PER_COUNT` (0 = encoder reads 0) |
| Turret direction | | FORWARD turns clockwise for positive degrees | Confirmed (team commit b126a0f, 2026-11-13); encoder sign still to check | `Turret` (NextMotor default) |
| Flywheel motor | Control Hub, motor port 3 | Velocity from motor's own encoder, 764 counts/rev (team, 2026-11-13); set by team | Pending (wire and check) | `ControlHub.SHOOTER_MOTOR` |
| Flywheel max speed | | 2100 RPM at encoder shaft (team, 2026-11-13) | Confirmed (team) | `Shooter.RADIANS_PER_COUNT`, `MAX_RPM` |
| Hood servo | Control Hub, servo port 4 | AXON MAX in continuous mode (team, 2026-11-13) | Pending (wire and check) | `ControlHub.HOOD_SERVO` |
| Hood servo feedback | Control Hub, analog input 0 | Axon 4th wire, 0..3.3 V = 0..360° assumed (NextFTC default) | Confirmed port (team, 2026-11-13); voltage span unverified | `ControlHub.HOOD_FEEDBACK_ANALOG` |
| Hood range | | Unwrapped servo degrees, more than one turn (format 40..420) | Pending (measure, 5.2a) | `Shooter.HOOD_MIN_DEG` / `HOOD_MAX_DEG` |
| Hood power direction | | Positive power raises feedback angle? | Pending | `Shooter.hoodReversed` |
| Rail servo | Control Hub, servo port 5 | Set by team (2026-11-13) | Pending (wire and check) | `ControlHub.RAIL_SERVO` |
| Rail positions | | POLLEN and NECTAR both 0 until measured | Pending | `Shooter.railPollen`, `railNectar` |
| Guard servo | Expansion Hub, servo port 5 | 0.55 = closed (balls wait), 0.78 = open (team, 2026-11-13) | Pending (wire and check) | `ExpansionHub.GUARD_SERVO`, `Guard.CLOSED/OPEN` |
| Limelight 3A | config name `"limelight"` | Pipeline 0 | Pending | `Limelight` |
| goBILDA RGB Indicator Light (3118-0808-0002) | Expansion Hub, servo port 0 | Set by team (2026-11-13) | Pending (wire and check) | `ExpansionHub.INDICATOR_SERVO` |
| Gate servo | Expansion Hub, servo port 4 | Position 0..1; set by team (2026-11-13) | Pending (wire and check) | `ExpansionHub.GATE_SERVO` |
| Gate color/distance sensor | Control Hub, I2C bus 2 | REV Color Sensor V2; addressed by bus, no Robot Configuration name | Pending (wire and check) | `ControlHub.GATE_COLOR_SENSOR_I2C_BUS` |
| Drive motors | config names `"frontLeft"`, `"frontRight"`, `"backLeft"`, `"backRight"` | All REVERSE (team, 2026-11-13) | Confirmed (team) | `pedro/Constants.driveConfig` |
| goBILDA Pinpoint | config name `"pinpoint"` | 4-bar pods, both FORWARD; pod offsets x 0.0051 in, y -2.078 in (team, 2026-11-13) | Confirmed (team) | `pedro/Constants.localizerConfig` |

Control Hub servo ports 0-3 broken (reported 2026-11-11); use Control Hub ports 4-5 or Expansion Hub ports.

Port-addressed devices not checked against Robot Configuration. If named device also configured on port
code addresses (e.g. Control Hub motor port 2 or I2C bus 2), both drive same device, NextFTC does not
detect conflict.
