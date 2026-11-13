package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import com.pedropathing.follower.Follower
import com.pedropathing.ivy.Command
import com.pedropathing.math.Pose
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.MotorPositionConstants
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import dev.nextftc.units.Degrees
import dev.nextftc.units.Radians
import dev.nextftc.units.radians
import kotlin.math.PI
import kotlin.math.abs
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
        // turret axis, robot frame (+y left), negative; unmeasured, lookAt holds until set
        @JvmField var AXIS_Y_IN = Double.NaN
        @JvmField var gains = MotorPositionConstants()
        @JvmField var leadS = 0.0 // aim from where the chassis will be this far ahead; 0 = no lead
        @JvmField var alignedRad = PI / 90 // 2°: shots wait until the turret is this close to its target
        @JvmField var seekRangeRad = PI / 6 // 30°: see chooseTurretAngle
    }

    var targetRad = 0.0
        private set

    fun setTarget(rad: Double, seekRange: Boolean = false) {
        targetRad = chooseTurretAngle(rad, targetRad, MIN_RAD, MAX_RAD, if (seekRange) seekRangeRad else null)
    }

    val aligned get() = abs(motor.encoderPosition.into(Radians) - targetRad) < alignedRad

    private val motor = NextMotor(RobotController.expansionHub, ExpansionHub.TURRET_MOTOR, RADIANS_PER_COUNT.radians)

    init {
        gains = motor.positionConstants
    }

    private fun aim() = motor.setPositionSetpoint(targetRad.radians)

    val fixedAngle = infinite { aim() }

    fun step(rad: Double) = instant { setTarget(targetRad + rad) }

    // An OpMode sets this during INIT to make it the default from START (FullTest: aim at the HIVE). START takes it
    // once, so an OpMode that doesn't set it gets fixedAngle, not a command built on an earlier OpMode's follower.
    var defaultFromStart: Command? = null

    override val defaultCommand: Command
        get() = (defaultFromStart ?: fixedAngle).also { defaultFromStart = null }

    // target is read every loop. While not firing, the turret unwinds early to keep travel for the next shot.
    fun lookAt(follower: Follower, firing: Boolean = false, target: () -> Pose) = infinite {
        val p = follower.pose()
        val v = follower.velocity() // field frame: in/s, rad/s
        val ahead = Pose(p.x() + v.vx * leadS, p.y() + v.vy * leadS, p.heading() + v.omega * leadS)
        setTarget(turretAngleTo(ahead, target(), AXIS_Y_IN), seekRange = !firing)
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
    val axis = turretAxis(pose, axisY)
    return pose.heading() - atan2(target.y() - axis.y(), target.x() - axis.x())
}

fun turretAxis(pose: Pose, axisY: Double) =
    Pose(pose.x() - axisY * sin(pose.heading()), pose.y() + axisY * cos(pose.heading()))

// Turret angle (rad) for direction rad: the equivalent angle (rad ± 2π) nearest the current one; if that is past a
// cable limit, unwind the other way round; if no equivalent is reachable, park at the nearer limit. A non-finite rad
// (unset offset, bad pose) keeps the current angle. With seekRange set, unwind early instead: once current is more than
// 180° + seekRange from the middle of the range, take the equivalent nearest the middle (most travel both ways). Going
// back needs the target to move 2 × seekRange, so it doesn't flip back and forth. Only matters for ranges over 360°.
fun chooseTurretAngle(rad: Double, current: Double, min: Double, max: Double, seekRange: Double? = null): Double {
    if (!rad.isFinite()) return current
    val mid = (min + max) / 2
    val from = if (seekRange != null && abs(current - mid) > PI + seekRange) mid else current
    val nearest = from + (rad - from + PI).mod(2 * PI) - PI
    val otherWay = if (nearest > from) nearest - 2 * PI else nearest + 2 * PI
    return when {
        nearest in min..max -> nearest
        otherWay in min..max -> otherWay
        (nearest - max).mod(2 * PI) < (min - nearest).mod(2 * PI) -> max
        else -> min
    }
}
