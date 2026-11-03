package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import com.pedropathing.math.Pose
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.MotorPositionConstants
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import dev.nextftc.units.Degrees
import dev.nextftc.units.degrees
import kotlin.math.atan2

@Configurable
class Turret : Mechanism {
    companion object {
        const val MOTOR_PORT = 2
        const val DEGREES_PER_COUNT = 0.0
        const val MIN_DEG = -60.0
        const val MAX_DEG = 60.0
        const val STEP_DEG = 10.0
        @JvmField var gains = MotorPositionConstants()
        @JvmField var lookX = 0.0
        @JvmField var lookY = 0.0
    }

    var targetDeg = 0.0
        set(deg) {
            val wrapped = (deg - MIN_DEG).mod(360.0) + MIN_DEG
            field = if (wrapped > (MIN_DEG + MAX_DEG + 360) / 2) MIN_DEG else wrapped.coerceAtMost(MAX_DEG)
        }

    private val motor = NextMotor(RobotController.expansionHub, MOTOR_PORT, DEGREES_PER_COUNT.degrees)

    init {
        gains = motor.positionConstants
    }

    private fun aim() = motor.setPositionSetpoint(targetDeg.degrees)

    val fixedAngle = infinite { aim() }

    fun step(deg: Double) = instant { targetDeg += deg }

    override val defaultCommand = fixedAngle

    //assumes the turret axis sits on the pinpoint origin
    fun lookAt(pose: () -> Pose) = infinite {
        val p = pose()
        targetDeg = Math.toDegrees(p.heading() - atan2(lookY - p.y(), lookX - p.x()))
        aim()
    }

    override fun periodic() {
        Telemetry.log("Turret deg", motor.encoderPosition.into(Degrees))
        Telemetry.log("Turret target deg", targetDeg)
    }
}
