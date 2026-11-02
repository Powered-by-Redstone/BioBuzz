package org.firstinspires.ftc.teamcode.opModes

import com.pedropathing.follower.Follower
import com.pedropathing.follower.ManualDrive
import dev.nextftc.robot.Telemetry
import dev.nextftc.robot.opmode.BulkReadHook
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextTeleop
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.pedro.Constants

@NextTeleop("Drive test")
class DriveTest(robot: Robot) : NextOpMode(robot, BulkReadHook) {
    private val follower: Follower = Constants.create(hardwareMap)

    override fun periodic() {
        // Pedro frame: +x forward, +y left, heading in radians, counter-clockwise positive.
        // Field-centric: stick up drives toward field +x regardless of robot heading.
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
