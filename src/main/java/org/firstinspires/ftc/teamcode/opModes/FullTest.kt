package org.firstinspires.ftc.teamcode.opModes

import com.pedropathing.follower.Follower
import com.pedropathing.follower.ManualDrive
import dev.nextftc.robot.Telemetry
import dev.nextftc.robot.opmode.BulkReadHook
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextUtility
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.mechanisms.Shooter
import org.firstinspires.ftc.teamcode.mechanisms.Turret
import org.firstinspires.ftc.teamcode.pedro.Constants

@NextUtility("Full test")
class FullTest(robot: Robot) : NextOpMode(robot, BulkReadHook) {
    private val follower: Follower = Constants.create(hardwareMap)

    init {
        robot.turret.targetDeg = 0.0
        robot.shooter.targetRpm = 0.0
        val gamepad = CommandGamepad(gamepad1)
        gamepad.square.toggleOnTrue(robot.intake.run)
        gamepad.circle.toggleOnTrue(robot.turret.lookAt { follower.pose() })
        gamepad.dpadLeft.onTrue(robot.turret.step(-Turret.STEP_DEG))
        gamepad.dpadRight.onTrue(robot.turret.step(Turret.STEP_DEG))
        gamepad.dpadUp.onTrue(robot.shooter.stepRpm(Shooter.RPM_STEP))
        gamepad.dpadDown.onTrue(robot.shooter.stepRpm(-Shooter.RPM_STEP))
        gamepad.rightBumper.onTrue(robot.shooter.stepHood(Shooter.HOOD_STEP))
        gamepad.leftBumper.onTrue(robot.shooter.stepHood(-Shooter.HOOD_STEP))
    }

    override fun periodic() {
        follower.manual(
            ManualDrive.fieldCentric(
                -gamepad1.left_stick_y.toDouble(),
                -gamepad1.left_stick_x.toDouble(),
                -gamepad1.right_stick_x.toDouble(),
                follower.pose().heading(),
            ),
        )
        follower.update()

        val pose = follower.pose()
        Telemetry.log("x (in)", pose.x())
        Telemetry.log("y (in)", pose.y())
        Telemetry.log("heading (deg)", Math.toDegrees(pose.heading()))
    }

    override fun end() {
        follower.drivetrain.stop()
    }
}
