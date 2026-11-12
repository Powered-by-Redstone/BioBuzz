package org.firstinspires.ftc.teamcode.opModes

import com.pedropathing.follower.Follower
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.mechanisms.Shooter
import org.firstinspires.ftc.teamcode.mechanisms.Turret

// Every gamepad mapping lives here, grouped by mechanism; each OpMode binds only the groups it needs.

fun CommandGamepad.bindCollection(robot: Robot) {
    square.toggleOnTrue(robot.intake.run)
    cross.toggleOnTrue(robot.transfer.run)
}

fun CommandGamepad.bindTurret(robot: Robot) {
    dpadLeft.onTrue(robot.turret.step(-Turret.STEP_RAD))
    dpadRight.onTrue(robot.turret.step(Turret.STEP_RAD))
}

fun CommandGamepad.bindAim(robot: Robot, follower: Follower) {
    circle.toggleOnTrue(robot.turret.lookAt(follower))
}

fun CommandGamepad.bindShooter(robot: Robot) {
    triangle.toggleOnTrue(robot.shooter.run)
    dpadUp.onTrue(robot.shooter.stepRpm(Shooter.RPM_STEP))
    dpadDown.onTrue(robot.shooter.stepRpm(-Shooter.RPM_STEP))
    rightBumper.onTrue(robot.shooter.stepHood(Shooter.HOOD_STEP))
    leftBumper.onTrue(robot.shooter.stepHood(-Shooter.HOOD_STEP))
    rightTrigger.isOver(0.5).onTrue(robot.shooter.switchBall)
}
