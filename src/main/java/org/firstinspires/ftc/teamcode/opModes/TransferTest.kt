package org.firstinspires.ftc.teamcode.opModes

import dev.nextftc.robot.opmode.BulkReadHook
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextUtility
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot

@NextUtility("Transfer test")
class TransferTest(robot: Robot) : NextOpMode(robot, BulkReadHook) {
    init {
        val gamepad = CommandGamepad(gamepad1)
        gamepad.square.toggleOnTrue(robot.transfer.run)
    }
}
