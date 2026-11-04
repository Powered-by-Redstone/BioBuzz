package org.firstinspires.ftc.teamcode

import com.bylazar.telemetry.PanelsTelemetry
import dev.nextftc.robot.NextRobot
import dev.nextftc.robot.Telemetry
import org.firstinspires.ftc.teamcode.mechanisms.Intake
import org.firstinspires.ftc.teamcode.mechanisms.Limelight
import org.firstinspires.ftc.teamcode.mechanisms.Shooter
import org.firstinspires.ftc.teamcode.mechanisms.Transfer
import org.firstinspires.ftc.teamcode.mechanisms.Turret

class Robot : NextRobot {
    val limelight = Limelight()
    val intake = Intake()
    val transfer = Transfer()
    val turret = Turret()
    val shooter = Shooter()
    override val mechanisms = setOf(limelight, intake, transfer, turret, shooter)

    init {
        Telemetry.addBackend(PanelsTelemetry.ftcTelemetry)
    }
}