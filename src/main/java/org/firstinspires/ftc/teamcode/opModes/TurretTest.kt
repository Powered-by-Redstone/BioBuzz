package org.firstinspires.ftc.teamcode.opModes

import dev.nextftc.robot.opmode.BulkReadHook
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextUtility
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot

@NextUtility("Turret test")
class TurretTest(robot: Robot) : NextOpMode(robot, BulkReadHook) {
    init {
        robot.turret.targetRad = 0.0
        robot.shooter.targetRpm = 0.0
        val gamepad = CommandGamepad(gamepad1)
        gamepad.bindTurret(robot)
        gamepad.bindShooter(robot)
    }
}
