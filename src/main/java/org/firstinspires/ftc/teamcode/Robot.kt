package org.firstinspires.ftc.teamcode

import com.bylazar.telemetry.PanelsTelemetry
import dev.nextftc.robot.NextRobot
import dev.nextftc.robot.Telemetry
import org.firstinspires.ftc.teamcode.mechanisms.Intake
import org.firstinspires.ftc.teamcode.mechanisms.LLTest
import org.firstinspires.ftc.teamcode.mechanisms.Shooter
import org.firstinspires.ftc.teamcode.mechanisms.Turret

class Robot : NextRobot {
    val llTest = LLTest()
    val intake = Intake()
    val turret = Turret()
    val shooter = Shooter()
    override val mechanisms = setOf(llTest, intake, turret, shooter)

    init {
        Telemetry.addBackend(PanelsTelemetry.ftcTelemetry)
    }
}