package org.firstinspires.ftc.teamcode.opModes

import dev.nextftc.robot.opmode.BulkReadHook
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextUtility
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.mechanisms.Shooter
import org.firstinspires.ftc.teamcode.mechanisms.Turret

@NextUtility("Turret test")
class TurretTest(robot: Robot) : NextOpMode(robot, BulkReadHook) {
    init {
        robot.turret.targetDeg = 0.0
        robot.shooter.targetRpm = 0.0
        val gamepad = CommandGamepad(gamepad1)
        gamepad.dpadLeft.onTrue(robot.turret.step(-Turret.STEP_DEG))
        gamepad.dpadRight.onTrue(robot.turret.step(Turret.STEP_DEG))
        gamepad.dpadUp.onTrue(robot.shooter.stepRpm(Shooter.RPM_STEP))
        gamepad.dpadDown.onTrue(robot.shooter.stepRpm(-Shooter.RPM_STEP))
        gamepad.rightBumper.onTrue(robot.shooter.stepHood(Shooter.HOOD_STEP))
        gamepad.leftBumper.onTrue(robot.shooter.stepHood(-Shooter.HOOD_STEP))
    }
}
