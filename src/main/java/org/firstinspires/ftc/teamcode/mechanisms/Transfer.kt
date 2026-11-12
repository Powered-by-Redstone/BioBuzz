package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import org.firstinspires.ftc.teamcode.ControlHub

@Configurable
class Transfer : Mechanism {
    companion object {
        @JvmField var power = 1.0
    }

    private val motor = NextMotor(RobotController.controlHub, ControlHub.TRANSFER_MOTOR)

    val run = infinite { motor.throttle = power.coerceIn(-1.0..1.0) }.setEnd { motor.throttle = 0.0 }

    override fun periodic() {
        Telemetry.log("Transfer throttle", motor.throttle)
    }
}
