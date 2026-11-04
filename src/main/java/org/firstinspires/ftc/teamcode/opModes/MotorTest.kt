package org.firstinspires.ftc.teamcode.opModes

import com.bylazar.configurables.annotations.Configurable
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.Utility
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import dev.nextftc.hardware.RobotController

@Configurable
@Utility(name = "Motor test", description = "Drive one motor from the left stick; hub and port set in Panels")
class MotorTest : LinearOpMode() {
    companion object {
        @JvmField var expansionHub = false
        @JvmField var port = 0
    }

    override fun runOpMode() {
        val hub = if (expansionHub) RobotController.expansionHub else RobotController.controlHub
        val motor = DcMotorImplEx(hub.motorController, port)
        waitForStart()
        while (opModeIsActive()) {
            motor.power = -gamepad1.left_stick_y.toDouble()
            telemetry.addData("Hub", if (expansionHub) "Expansion" else "Control")
            telemetry.addData("Port", port)
            telemetry.addData("Power", motor.power)
            telemetry.addData("Position (counts)", motor.currentPosition)
            telemetry.addData("Velocity (counts/s)", motor.velocity)
            telemetry.update()
        }
        motor.power = 0.0
    }
}
