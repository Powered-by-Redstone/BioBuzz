package org.firstinspires.ftc.teamcode.opModes

import com.bylazar.configurables.annotations.Configurable
import com.pedropathing.follower.Follower
import com.pedropathing.ivy.commands.Commands
import com.pedropathing.math.Pose
import dev.nextftc.robot.Telemetry
import dev.nextftc.robot.coroutine.command
import dev.nextftc.robot.opmode.BulkReadHook
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextUtility
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.mechanisms.Guard
import org.firstinspires.ftc.teamcode.pedro.Constants

// Turret PID: dpad left/right step the target by Turret.STEP_RAD; tune Turret.gains in Panels against
// "Turret deg" / "Turret target deg".
// Launch tables: enter the start pose in Panels during INIT, push the robot to a spot, circle aims at the HIVE, set RPM
// (dpad up/down) and hood (bumpers), right trigger switches the ball type, cross fires one ball once "Ready to fire".
// A shot that scores is one table row: Shot("Hive distance (in)", "Shooter target rpm", "Hood position").
@Configurable
@NextUtility("Turret test", "Turret PID and launch tables: dpad turret/RPM, bumpers hood, circle aims, cross fires")
class TurretTest(private val robot: Robot) : NextOpMode(robot, BulkReadHook) {
    companion object {
        // where the robot stands at START (Pedro inches, radians); NaN = unknown, so the turret doesn't aim at the HIVE
        @JvmField var startX = Double.NaN
        @JvmField var startY = Double.NaN
        @JvmField var startHeadingRad = Double.NaN
    }

    // null while Pedro is unconfigured (Constants.create returns null): turret PID and manual shots still work
    private val follower: Follower? = Constants.create(hardwareMap)

    private var placed = false

    // one ball at the current RPM and hood, released like Robot.shoot but without the tables or Robot.balls
    private val fireOne = command {
        awaitAny(robot.transfer.run, Commands.waitUntil { robot.shooter.atSpeed && robot.turret.aligned })
        awaitAny(
            robot.transfer.feed,
            robot.guard.open,
            Commands.waitUntil { robot.shooter.dipped },
            Commands.waitMs(Guard.maxOpenS * 1000),
        )
    }.requiring(robot.guard, robot.transfer)

    init {
        robot.turret.setTarget(0.0)
        robot.shooter.targetRpm = 0.0
        val gamepad = CommandGamepad(gamepad1)
        gamepad.bindTurret(robot)
        gamepad.bindShooter(robot)
        gamepad.bindCalibration(
            fireOne,
            follower?.let {
                robot.turret.lookAt(it, firing = true) { if (placed) robot.hive() else Pose(Double.NaN, Double.NaN) }
            },
        )
    }

    override fun start() {
        placed = startX.isFinite() && startY.isFinite() && startHeadingRad.isFinite()
        if (placed) follower?.setPose(Pose(startX, startY, startHeadingRad))
    }

    override fun periodic() {
        follower?.update()
        Telemetry.log("Ready to fire", robot.shooter.atSpeed && robot.turret.aligned)
        Telemetry.log(
            "Hive distance (in)",
            if (follower != null && placed) robot.hiveDistance(follower) else "needs Pedro and a start pose",
        )
    }

    override fun end() {
        follower?.drivetrain?.stop()
    }
}
