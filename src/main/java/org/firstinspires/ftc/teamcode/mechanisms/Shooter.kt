package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import com.pedropathing.ivy.commands.Commands
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.MotorVelocityConstants
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.hardware.actuators.NextServo
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import dev.nextftc.units.RotationsPerMinute
import dev.nextftc.units.radians
import dev.nextftc.units.rpm
import kotlin.math.PI
import kotlin.math.abs
import org.firstinspires.ftc.teamcode.ExpansionHub

enum class BallType { POLLEN, NECTAR }

// distanceIn: turret axis to HIVE scoring point (inches); rpm: flywheel motor shaft; hood: servo position 0..1
data class Shot(val distanceIn: Double, val rpm: Double, val hood: Double)

@Configurable
class Shooter : Mechanism {
    companion object {
        const val RADIANS_PER_COUNT = 2 * PI / 28 // motor encoder, so RPM is at the motor shaft
        const val MAX_RPM = 6000.0
        const val RPM_STEP = 100.0
        const val HOOD_STEP = 0.05
        @JvmField var gains = MotorVelocityConstants()
        @JvmField var railPollen = 0.0 // servo position 0..1
        @JvmField var railNectar = 0.0 // servo position 0..1
        @JvmField var rpmTolerance = 100.0 // shots wait until measured RPM is this close to the target
        @JvmField var shotDipRpm = 300.0 // CALIBRATION REQUIRED: a drop this far below target means a ball went through

        // MEASUREMENT REQUIRED: one Shot per measured distance, increasing distance. Beyond either end the end entry is
        // used; an empty table blocks firing.
        val POLLEN_SHOTS = listOf<Shot>()
        val NECTAR_SHOTS = listOf<Shot>()
    }

    var targetRpm = 0.0
        set(rpm) {
            field = rpm.coerceIn(0.0, MAX_RPM)
        }

    var hood = 0.0
        set(position) {
            field = position.coerceIn(0.0, 1.0)
        }

    var ball = BallType.NECTAR

    private val motor = NextMotor(RobotController.expansionHub, ExpansionHub.SHOOTER_MOTOR, RADIANS_PER_COUNT.radians)
    private val servo = NextServo(RobotController.expansionHub, ExpansionHub.HOOD_SERVO)
    private val rail = NextServo(RobotController.expansionHub, ExpansionHub.RAIL_SERVO)

    init {
        gains = motor.velocityConstants
    }

    // coast at 0 instead of letting the pid brake the flywheel with reverse power
    val run = infinite {
        if (targetRpm == 0.0) motor.throttle = 0.0 else motor.setVelocitySetpoint(targetRpm.rpm)
        servo.position = hood
        rail.position = (if (ball == BallType.NECTAR) railNectar else railPollen).coerceIn(0.0, 1.0)
    }.setEnd { motor.throttle = 0.0 }

    // targetRpm outlives OpModes (one shared robot), so every OpMode starts with the flywheel at 0
    override val defaultCommand = instant { targetRpm = 0.0 }.then(run)

    val atSpeed get() = targetRpm > 0 && abs(rpm - targetRpm) <= rpmTolerance

    // never below rpmTolerance, so a flywheel that is at speed can't count as a shot
    val dipped get() = rpm < targetRpm - maxOf(shotDipRpm, rpmTolerance)

    private val rpm get() = motor.encoderVelocity.into(RotationsPerMinute)

    // Sets RPM and hood for `ball` at distanceIn; false (setpoints unchanged) for an unknown distance or unusable table.
    fun aimFor(distanceIn: Double): Boolean {
        val shot = interpolate(if (ball == BallType.POLLEN) POLLEN_SHOTS else NECTAR_SHOTS, distanceIn) ?: return false
        targetRpm = shot.rpm
        hood = shot.hood
        return true
    }

    // these only change setpoints, so they don't require the shooter and can't interrupt run
    fun stepRpm(rpm: Double) = Commands.instant { targetRpm += rpm }

    fun stepHood(position: Double) = Commands.instant { hood += position }

    val switchBall = Commands.instant { ball = if (ball == BallType.POLLEN) BallType.NECTAR else BallType.POLLEN }

    override fun periodic() {
        Telemetry.log("Shooter rpm", rpm)
        Telemetry.log("Shooter target rpm", targetRpm)
        Telemetry.log("Hood position", hood)
        Telemetry.log("Rail ball", ball)
    }
}

// Linear interpolation between the two entries around distanceIn; outside the table, the nearest end entry (team,
// 2026-11-12). Null for NaN, an empty table, or distances that are not strictly increasing.
fun interpolate(table: List<Shot>, distanceIn: Double): Shot? {
    val pairs = table.zipWithNext()
    if (table.isEmpty() || distanceIn.isNaN() || pairs.any { (a, b) -> b.distanceIn <= a.distanceIn }) return null
    val d = distanceIn.coerceIn(table.first().distanceIn, table.last().distanceIn)
    val (a, b) = pairs.firstOrNull { (_, b) -> d <= b.distanceIn } ?: return table.single()
    val t = (d - a.distanceIn) / (b.distanceIn - a.distanceIn)
    return Shot(d, a.rpm + t * (b.rpm - a.rpm), a.hood + t * (b.hood - a.hood))
}
