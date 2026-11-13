package org.firstinspires.ftc.teamcode

import com.bylazar.telemetry.PanelsTelemetry
import com.pedropathing.follower.Follower
import com.pedropathing.ivy.behaviors.ConflictBehavior
import com.pedropathing.ivy.commands.Commands
import com.pedropathing.math.Pose
import dev.nextftc.robot.NextRobot
import dev.nextftc.robot.Telemetry
import dev.nextftc.robot.coroutine.command
import org.firstinspires.ftc.teamcode.mechanisms.Alliance
import org.firstinspires.ftc.teamcode.mechanisms.BallType
import org.firstinspires.ftc.teamcode.mechanisms.Gate
import org.firstinspires.ftc.teamcode.mechanisms.Guard
import org.firstinspires.ftc.teamcode.mechanisms.HiveState
import org.firstinspires.ftc.teamcode.mechanisms.Indicator
import org.firstinspires.ftc.teamcode.mechanisms.Intake
import org.firstinspires.ftc.teamcode.mechanisms.Limelight
import org.firstinspires.ftc.teamcode.mechanisms.Shooter
import org.firstinspires.ftc.teamcode.mechanisms.Transfer
import org.firstinspires.ftc.teamcode.mechanisms.Turret
import org.firstinspires.ftc.teamcode.mechanisms.turretAxis

class Robot : NextRobot {
    companion object {
        const val MAX_BALLS = 4 // game manual G407: a robot may not control more than 4 scoring elements

        // Center of the up-facing CELL opening per HIVE state, Pedro inches as red sees the field from the audience
        // (origin at the audience-side corner on red's side, +y away from the audience). The field is symmetric under
        // a 180° turn, so blue uses the same numbers. From manual §9.6: HIVE centers 25.5 in apart about the field
        // center, CELL opening center 15.8 in from the pivot along the HIVE (30° tilt).
        val AUDIENCE_HIVE = Pose(59.25, 56.2)
        val COUNTER_HIVE = Pose(59.25, 87.8)
    }

    // set by the alliance's autonomous and kept for TeleOp; RED until one runs
    var alliance = Alliance.RED

    // accepted balls in the transfer, first = next to fire; outlives OpModes like the robot
    val balls = ArrayDeque<BallType>()

    val limelight = Limelight { alliance }
    val intake = Intake()
    val transfer = Transfer()
    val turret = Turret()
    val shooter = Shooter()
    val indicator = Indicator { state() }
    val gate = Gate()
    val guard = Guard()
    override val mechanisms = setOf(limelight, intake, transfer, turret, shooter, indicator, gate, guard)

    init {
        Telemetry.addBackend(PanelsTelemetry.ftcTelemetry)
    }

    // The robot starts every match with exactly 4 POLLEN (manual §10.3.4), all in the transfer, gate empty
    // (team, 2026-11-12).
    fun preload() {
        balls.clear()
        repeat(4) { balls.addLast(BallType.POLLEN) }
    }

    // what the indicator shows, highest priority first
    fun state() = when {
        guard.isOpen -> Indicator.State.SHOOTING
        intake.rejecting -> Indicator.State.REJECTING
        intake.intaking -> Indicator.State.INTAKING
        balls.isNotEmpty() && limelight.hiveState == HiveState.NA -> Indicator.State.NO_HIVE
        balls.size >= MAX_BALLS -> Indicator.State.FULL
        balls.isNotEmpty() -> Indicator.State.WAITING_TO_SHOOT
        else -> Indicator.State.IDLE
    }

    fun hive() = when (limelight.hiveState) {
        HiveState.AUDIENCE -> AUDIENCE_HIVE
        HiveState.COUNTER -> COUNTER_HIVE
        HiveState.NA -> Pose(Double.NaN, Double.NaN)
    }

    fun hiveDistance(follower: Follower) = turretAxis(follower.pose(), Turret.AXIS_Y_IN).distance(hive())

    // Intakes until MAX_BALLS are accepted. Keeps POLLEN and our NECTAR, queueing one shot each; spits out theirs.
    fun collect(follower: Follower) = command {
        while (balls.size < MAX_BALLS) {
            await(gate.open)
            awaitAny(intake.run, Commands.waitUntil { gate.hasBall })
            await(gate.hold)
            // held still, so the color is read here; still unknown after Gate.colorTimeoutS: rejected below
            awaitAny(Commands.waitUntil { gate.color != null }, Commands.waitMs(Gate.colorTimeoutS * 1000))
            val color = gate.color
            val ours = if (alliance == Alliance.RED) Gate.RED else Gate.BLUE
            if (color == Gate.YELLOW || color == ours) {
                // counted before closing: an interrupt mid-close must not leave a ball in the transfer uncounted (G407)
                balls.addLast(if (color == Gate.YELLOW) BallType.POLLEN else BallType.NECTAR)
                shoot(follower).schedule()
                await(gate.close)
                wait(Gate.transferS)
            } else {
                await(gate.open) // let go of the ball before pushing it back out
                await(intake.reject)
            }
        }
    }.requiring(intake, gate)

    // Fires the first ball once the flywheel is at speed for its distance and the turret is on the HIVE: opens the
    // guard with the transfer slowed until the flywheel dips (ball shot) or Guard.maxOpenS passes (closes and retries).
    // Shots queue behind each other in order, and take the turret from manual commands.
    fun shoot(follower: Follower) = command {
        shooter.ball = balls.firstOrNull() ?: return@command
        val aim = turret.lookAt(follower, firing = true) { hive() }
        do {
            awaitAny(
                aim,
                transfer.run,
                Commands.waitUntil { shooter.aimFor(hiveDistance(follower)) && shooter.ready && turret.aligned },
            )
            val dip = Commands.waitUntil { shooter.dipped }
        } while (awaitAny(aim, transfer.feed, guard.open, dip, Commands.waitMs(Guard.maxOpenS * 1000)) != dip)
        balls.removeFirst()
    }.requiring(turret, guard, transfer).setPriority(1).setConflictBehavior(ConflictBehavior.QUEUE)

    override fun periodic() {
        Telemetry.log("Alliance", alliance)
        Telemetry.log("Balls", balls)
    }
}
