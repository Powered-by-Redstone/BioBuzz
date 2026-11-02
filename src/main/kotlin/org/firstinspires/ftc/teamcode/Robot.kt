package org.firstinspires.ftc.teamcode

import dev.nextftc.robot.NextRobot
import org.firstinspires.ftc.teamcode.Mechanisms.LLTest

class Robot : NextRobot {
    val llTest = LLTest()
    override val mechanisms = setOf(llTest)
}