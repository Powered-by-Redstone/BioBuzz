package org.firstinspires.ftc.teamcode.opModes

import dev.nextftc.robot.opmode.BulkReadHook
import dev.nextftc.robot.opmode.NextAutonomous
import dev.nextftc.robot.opmode.NextOpMode
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.mechanisms.Alliance

// Placeholders: for now they only set the alliance and the preloads, which TeleOp keeps.

@NextAutonomous("Red")
class RedAuto(robot: Robot) : NextOpMode(robot, BulkReadHook) {
    init {
        robot.alliance = Alliance.RED
        robot.preload()
    }
}

@NextAutonomous("Blue")
class BlueAuto(robot: Robot) : NextOpMode(robot, BulkReadHook) {
    init {
        robot.alliance = Alliance.BLUE
        robot.preload()
    }
}
