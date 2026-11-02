package org.firstinspires.ftc.teamcode

import dev.nextftc.robot.NextRobot
import org.firstinspires.ftc.teamcode.mechanisms.Intake
import org.firstinspires.ftc.teamcode.mechanisms.LLTest

class Robot : NextRobot {
    val llTest = LLTest()
    val intake = Intake()
    override val mechanisms = setOf(llTest, intake)
}