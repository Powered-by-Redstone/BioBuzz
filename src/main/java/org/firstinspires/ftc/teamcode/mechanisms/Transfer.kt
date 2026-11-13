package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import org.firstinspires.ftc.teamcode.ExpansionHub

@Configurable
class Transfer : Mechanism {
    companion object {
        @JvmField var power = 1.0
        // CALIBRATION REQUIRED: slow enough that the next ball doesn't follow into the flywheel
        @JvmField var feedPower = 0.5
    }

    private val motor = NextMotor(RobotController.expansionHub, ExpansionHub.TRANSFER_MOTOR)

    val run = infinite { motor.throttle = power.coerceIn(-1.0..1.0) }.setEnd { motor.throttle = 0.0 }

    val feed = infinite { motor.throttle = feedPower.coerceIn(-1.0..1.0) }.setEnd { motor.throttle = 0.0 }

    override val defaultCommand = run

    override fun periodic() {
        Telemetry.log("Transfer throttle", motor.throttle)
    }
}
