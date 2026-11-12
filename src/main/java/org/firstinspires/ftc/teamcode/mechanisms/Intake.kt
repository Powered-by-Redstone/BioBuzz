package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import org.firstinspires.ftc.teamcode.ControlHub

@Configurable
class Intake : Mechanism {
    companion object {
        @JvmField var power = 1.0
        @JvmField var rejectS = 0.5 // CALIBRATION REQUIRED: reverse long enough to push a ball back out
    }

    private val motor = NextMotor(RobotController.controlHub, ControlHub.INTAKE_MOTOR)

    val run = infinite { motor.throttle = power.coerceIn(-1.0..1.0) }.setEnd { motor.throttle = 0.0 }

    val reject = coroutine {
        try {
            motor.throttle = -power.coerceIn(-1.0..1.0)
            wait(rejectS)
        } finally {
            motor.throttle = 0.0
        }
    }

    override fun periodic() {
        Telemetry.log("Intake throttle", motor.throttle)
    }
}
