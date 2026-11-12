package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import com.pedropathing.follower.Follower
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
import kotlin.math.cos
import kotlin.math.sin
import org.firstinspires.ftc.teamcode.ExpansionHub

@Configurable
class Turret : Mechanism {
    companion object {
        const val RADIANS_PER_COUNT = 0.0
        @JvmField var MIN_RAD = -PI / 3 // -60°
        @JvmField var MAX_RAD = PI / 3 // 60°
        @JvmField var STEP_RAD = PI / 18 // 10°
        @JvmField var AXIS_Y_IN = Double.NaN // turret axis, robot frame (+y left), negative; unmeasured, lookAt holds until set
        @JvmField var gains = MotorPositionConstants()
        @JvmField var lookX = 0.0
        @JvmField var lookY = 0.0
        @JvmField var leadS = 0.0 // aim from where the chassis will be this far ahead; 0 = no lead
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

    // null target reads Panels lookX/lookY every loop, so edits apply live
    fun lookAt(follower: Follower, target: Pose? = null) = infinite {
        val p = follower.pose()
        val v = follower.velocity() // field frame: in/s, rad/s
        val ahead = Pose(p.x() + v.vx * leadS, p.y() + v.vy * leadS, p.heading() + v.omega * leadS)
        targetRad = turretAngleTo(ahead, target ?: Pose(lookX, lookY), AXIS_Y_IN)
        aim()
    }

    override fun periodic() {
        Telemetry.log("Turret deg", motor.encoderPosition.into(Degrees))
        Telemetry.log("Turret target deg", Math.toDegrees(targetRad))
        if (AXIS_Y_IN.isNaN()) Telemetry.log("Turret AXIS_Y_IN unset: lookAt holds")
    }
}

// Turret angle (rad, clockwise from robot forward) that points the turret axis at target. Pedro pose: field inches,
// heading in radians counter-clockwise; the axis sits at robot-frame (0, axisY) inches from the pose origin.
fun turretAngleTo(pose: Pose, target: Pose, axisY: Double): Double {
    val x = pose.x() - axisY * sin(pose.heading())
    val y = pose.y() + axisY * cos(pose.heading())
    return pose.heading() - atan2(target.y() - y, target.x() - x)
}

// Turret angle (rad) for direction rad: the equivalent angle (rad ± 2π) nearest the current one; if that is past a
// cable limit, unwind the other way round; if no equivalent is reachable, park at the nearer limit. A non-finite rad
// (unset offset, bad pose) keeps the current angle.
fun chooseTurretAngle(rad: Double, current: Double, min: Double, max: Double): Double {
    if (!rad.isFinite()) return current
    val nearest = current + (rad - current + PI).mod(2 * PI) - PI
    val otherWay = if (nearest > current) nearest - 2 * PI else nearest + 2 * PI
    return when {
        nearest in min..max -> nearest
        otherWay in min..max -> otherWay
        (nearest - max).mod(2 * PI) < (min - nearest).mod(2 * PI) -> max
        else -> min
    }
}
