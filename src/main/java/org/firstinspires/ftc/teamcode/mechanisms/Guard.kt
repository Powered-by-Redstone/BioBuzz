package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextServo
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import org.firstinspires.ftc.teamcode.ControlHub
import org.firstinspires.ftc.teamcode.ExpansionHub

// Flap between the transfer and the flywheel: closed, balls wait against it even while the transfer spins.
@Configurable
class Guard : Mechanism {
    companion object {
        const val CLOSED = 0.55
        const val OPEN = 0.78
        // CALIBRATION REQUIRED: a shot closes the guard and retries if the flywheel shows no dip this long after opening
        @JvmField var maxOpenS = 0.5
    }

    private val servo = NextServo(RobotController.expansionHub, ExpansionHub.GUARD_SERVO)

    override val defaultCommand = infinite { servo.position = CLOSED }

    // closes itself: the next queued shot keeps the guard, so the default command doesn't run in between
    val open = infinite { servo.position = OPEN }.setEnd { servo.position = CLOSED }

    val isOpen get() = servo.position == OPEN

    override fun periodic() {
        Telemetry.log("Guard position", servo.position)
    }
}
