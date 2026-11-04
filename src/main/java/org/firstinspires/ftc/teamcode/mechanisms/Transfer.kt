package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry

@Configurable
class Transfer : Mechanism {
    companion object {
        const val MOTOR_PORT = 3
        @JvmField var power = 1.0
    }

    private val motor = NextMotor(RobotController.controlHub, MOTOR_PORT)

    val run = infinite { motor.throttle = power.coerceIn(-1.0..1.0) }.setEnd { motor.throttle = 0.0 }

    override fun periodic() {
        Telemetry.log("Transfer throttle", motor.throttle)
    }
}
