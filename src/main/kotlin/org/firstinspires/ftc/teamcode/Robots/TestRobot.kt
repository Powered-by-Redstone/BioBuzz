package org.firstinspires.ftc.teamcode.Robots

import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.NextRobot
import org.firstinspires.ftc.teamcode.Mechanisms.LLTest

class TestRobot(): NextRobot {
    val llTest = LLTest()
    override val mechanisms = setOf(llTest)
}