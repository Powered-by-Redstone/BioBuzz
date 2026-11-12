package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import com.pedropathing.math.Pose
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.MotorPositionConstants
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import dev.nextftc.units.Degrees
import dev.nextftc.units.radians
import kotlin.math.PI
import kotlin.math.atan2
import org.firstinspires.ftc.teamcode.ExpansionHub

@Configurable
class Turret : Mechanism {
    companion object {
        const val RADIANS_PER_COUNT = 0.0
        const val MIN_RAD = -PI / 3 // -60°
        const val MAX_RAD = PI / 3 // 60°
        const val STEP_RAD = PI / 18 // 10°
        @JvmField var gains = MotorPositionConstants()
        @JvmField var lookX = 0.0
        @JvmField var lookY = 0.0
    }

    var targetRad = 0.0
        set(rad) {
            field = chooseTurretAngle(rad, field, MIN_RAD, MAX_RAD)
        }

    private val motor = NextMotor(RobotController.expansionHub, ExpansionHub.TURRET_MOTOR, RADIANS_PER_COUNT.radians)

    init {
        gains = motor.positionConstants
    }

    private fun aim() = motor.setPositionSetpoint(targetRad.radians)

    val fixedAngle = infinite { aim() }

    fun step(rad: Double) = instant { targetRad += rad }

    override val defaultCommand = fixedAngle

    //assumes the turret axis sits on the pinpoint origin
    // null target reads Panels lookX/lookY every loop, so edits apply live
    fun lookAt(target: Pose? = null, pose: () -> Pose) = infinite {
        val p = pose()
        val t = target ?: Pose(lookX, lookY)
        targetRad = p.heading() - atan2(t.y() - p.y(), t.x() - p.x())
        aim()
    }

    override fun periodic() {
        Telemetry.log("Turret deg", motor.encoderPosition.into(Degrees))
        Telemetry.log("Turret target deg", Math.toDegrees(targetRad))
    }
}

// Turret angle (rad) for direction rad: the equivalent angle (rad ± 2π) nearest the current one; if that is past a
// cable limit, unwind the other way round; if no equivalent is reachable, park at the nearer limit.
fun chooseTurretAngle(rad: Double, current: Double, min: Double, max: Double): Double {
    val nearest = current + (rad - current + PI).mod(2 * PI) - PI
    val otherWay = if (nearest > current) nearest - 2 * PI else nearest + 2 * PI
    return when {
        nearest in min..max -> nearest
        otherWay in min..max -> otherWay
        (nearest - max).mod(2 * PI) < (min - nearest).mod(2 * PI) -> max
        else -> min
    }
}
