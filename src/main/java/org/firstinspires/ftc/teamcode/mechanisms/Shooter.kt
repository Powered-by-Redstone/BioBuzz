package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.MotorVelocityConstants
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.hardware.actuators.NextServo
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import dev.nextftc.units.RotationsPerMinute
import dev.nextftc.units.degrees
import dev.nextftc.units.rpm

@Configurable
class Shooter : Mechanism {
    companion object {
        const val MOTOR_PORT = 3
        const val HOOD_PORT = 0
        const val DEGREES_PER_COUNT = 360.0 / 28 // motor encoder, so RPM is at the motor shaft
        const val MAX_RPM = 6000.0
        const val RPM_STEP = 100.0
        const val HOOD_STEP = 0.05
        @JvmField var gains = MotorVelocityConstants()
    }

    var targetRpm = 0.0
        set(rpm) {
            field = rpm.coerceIn(0.0, MAX_RPM)
        }

    var hood = 0.0
        set(position) {
            field = position.coerceIn(0.0, 1.0)
        }

    private val motor = NextMotor(RobotController.expansionHub, MOTOR_PORT, DEGREES_PER_COUNT.degrees)
    private val servo = NextServo(RobotController.expansionHub, HOOD_PORT)

    init {
        gains = motor.velocityConstants
    }

    // coast at 0 instead of letting the pid brake the flywheel with reverse power
    override val defaultCommand = infinite {
        if (targetRpm == 0.0) motor.throttle = 0.0 else motor.setVelocitySetpoint(targetRpm.rpm)
        servo.position = hood
    }

    fun stepRpm(rpm: Double) = instant { targetRpm += rpm }

    fun stepHood(position: Double) = instant { hood += position }

    override fun periodic() {
        Telemetry.log("Shooter rpm", motor.encoderVelocity.into(RotationsPerMinute))
        Telemetry.log("Shooter target rpm", targetRpm)
        Telemetry.log("Hood position", hood)
    }
}
