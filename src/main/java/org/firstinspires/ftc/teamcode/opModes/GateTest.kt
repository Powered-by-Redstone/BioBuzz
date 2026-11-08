package org.firstinspires.ftc.teamcode.opModes

import dev.nextftc.robot.opmode.BulkReadHook
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextUtility
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot

@NextUtility("Gate test")
class GateTest(robot: Robot) : NextOpMode(robot, BulkReadHook) {
    init {
        val gamepad = CommandGamepad(gamepad1)
        gamepad.cross.onTrue(robot.gate.open)
        gamepad.square.onTrue(robot.gate.close)
        gamepad.triangle.onTrue(robot.gate.hold)
    }
    override fun start() {
    }
}
