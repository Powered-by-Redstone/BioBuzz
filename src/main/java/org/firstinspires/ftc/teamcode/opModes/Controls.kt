package org.firstinspires.ftc.teamcode.opModes

import com.pedropathing.follower.Follower
import com.pedropathing.ivy.Command
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.mechanisms.Shooter
import org.firstinspires.ftc.teamcode.mechanisms.Turret

fun CommandGamepad.bindCollection(robot: Robot, follower: Follower) {
    square.toggleOnTrue(robot.collect(follower))
}

fun CommandGamepad.bindTurret(robot: Robot) {
    dpadLeft.onTrue(robot.turret.step(-Turret.STEP_RAD))
    dpadRight.onTrue(robot.turret.step(Turret.STEP_RAD))
}

fun CommandGamepad.bindShooter(robot: Robot) {
    dpadUp.onTrue(robot.shooter.stepRpm(Shooter.RPM_STEP))
    dpadDown.onTrue(robot.shooter.stepRpm(-Shooter.RPM_STEP))
    rightBumper.onTrue(robot.shooter.stepHood(Shooter.HOOD_STEP))
    leftBumper.onTrue(robot.shooter.stepHood(-Shooter.HOOD_STEP))
    rightTrigger.isOver(0.5).onTrue(robot.shooter.switchBall)
}

// TurretTest: cross fires one ball, circle toggles HIVE aiming (null without Pedro, so circle does nothing)
fun CommandGamepad.bindCalibration(fireOne: Command, aimAtHive: Command?) {
    cross.onTrue(fireOne)
    aimAtHive?.let { circle.toggleOnTrue(it) }
}

fun CommandGamepad.bindGate(robot: Robot) {
    cross.onTrue(robot.gate.open)
    square.onTrue(robot.gate.close)
    triangle.onTrue(robot.gate.hold)
    circle.onTrue(robot.gate.acceptBall)
}
