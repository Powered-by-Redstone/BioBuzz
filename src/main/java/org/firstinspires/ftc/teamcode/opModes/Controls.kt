package org.firstinspires.ftc.teamcode.opModes

import com.pedropathing.math.Pose
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.mechanisms.Shooter
import org.firstinspires.ftc.teamcode.mechanisms.Turret

fun CommandGamepad.bindCollection(robot: Robot) {
    square.toggleOnTrue(robot.intake.run)
    cross.toggleOnTrue(robot.transfer.run)
}

fun CommandGamepad.bindTurret(robot: Robot) {
    dpadLeft.onTrue(robot.turret.step(-Turret.STEP_RAD))
    dpadRight.onTrue(robot.turret.step(Turret.STEP_RAD))
}

fun CommandGamepad.bindAim(robot: Robot, pose: () -> Pose) {
    circle.toggleOnTrue(robot.turret.lookAt(pose = pose))
}

fun CommandGamepad.bindShooter(robot: Robot) {
    dpadUp.onTrue(robot.shooter.stepRpm(Shooter.RPM_STEP))
    dpadDown.onTrue(robot.shooter.stepRpm(-Shooter.RPM_STEP))
    rightBumper.onTrue(robot.shooter.stepHood(Shooter.HOOD_STEP))
    leftBumper.onTrue(robot.shooter.stepHood(-Shooter.HOOD_STEP))
}

fun CommandGamepad.bindGate(robot: Robot) {
    cross.onTrue(robot.gate.open)
    square.onTrue(robot.gate.close)
    triangle.onTrue(robot.gate.hold)
    circle.onTrue(robot.gate.acceptBall)
}
