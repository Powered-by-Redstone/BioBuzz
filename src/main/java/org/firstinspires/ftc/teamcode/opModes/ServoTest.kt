package org.firstinspires.ftc.teamcode.opModes

import com.bylazar.configurables.annotations.Configurable
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.Utility
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import dev.nextftc.hardware.RobotController

@Configurable
@Utility(name = "Servo test", description = "Step one servo with dpad up/down; hub, port and step set in Panels")
class ServoTest : LinearOpMode() {
    companion object {
        @JvmField var expansionHub = false
        @JvmField var port = 0
        @JvmField var step = 0.03
    }

    override fun runOpMode() {
        val hub = if (expansionHub) RobotController.expansionHub else RobotController.controlHub
        val servo = ServoImplEx(hub.servoController, port, ServoConfigurationType.getStandardServoType())
        var position = 0.5
        waitForStart()
        while (opModeIsActive()) {
            if (gamepad1.dpadUpWasPressed()) position += step
            if (gamepad1.dpadDownWasPressed()) position -= step
            position = position.coerceIn(0.0, 1.0)
            servo.position = position
            telemetry.addData("Hub", if (expansionHub) "Expansion" else "Control")
            telemetry.addData("Port", port)
            telemetry.addData("Position", servo.position)
            telemetry.update()
        }
        servo.setPwmDisable()
    }
}
