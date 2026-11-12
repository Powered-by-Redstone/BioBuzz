package org.firstinspires.ftc.teamcode.opModes

import dev.nextftc.robot.opmode.BulkReadHook
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextUtility
import org.firstinspires.ftc.teamcode.Robot

@NextUtility("Limelight test", "No controls: watch the Limelight HIVE state and tag telemetry")
class LimelightTest(robot: Robot) : NextOpMode(robot, BulkReadHook)
