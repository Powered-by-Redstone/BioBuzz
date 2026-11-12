package org.firstinspires.ftc.teamcode.opModes

import com.pedropathing.follower.Follower
import com.pedropathing.follower.ManualDrive
import dev.nextftc.robot.Telemetry
import dev.nextftc.robot.opmode.BulkReadHook
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextUtility
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.pedro.Constants

@NextUtility("Full test")
class FullTest(private val robot: Robot) : NextOpMode(robot, BulkReadHook) {
    private val follower: Follower = Constants.create(hardwareMap)

    init {
        robot.turret.setTarget(0.0)
        robot.turret.defaultFromStart = robot.turret.lookAt(follower) { robot.hive() }
        robot.shooter.targetRpm = 0.0
        val gamepad = CommandGamepad(gamepad1)
        gamepad.bindCollection(robot, follower)
        gamepad.bindShooter(robot)
    }

    // queued shots end with the previous OpMode, so balls already counted (preloads, leftovers) get one each here
    override fun start() {
        repeat(robot.balls.size) { robot.shoot(follower).schedule() }
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
        Telemetry.log("Hive distance (in)", robot.hiveDistance(follower))
    }

    override fun end() {
        follower.drivetrain.stop()
    }
}
