package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import com.pedropathing.ivy.commands.Commands
import dev.nextftc.control.feedback.PIDCoefficients
import dev.nextftc.control.feedback.PIDController
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.MotorVelocityConstants
import dev.nextftc.hardware.actuators.NextFeedbackCRServo
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.hardware.actuators.NextServo
import dev.nextftc.hardware.sensors.NextAnalogInput
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import dev.nextftc.units.Degrees
import dev.nextftc.units.RotationsPerMinute
import dev.nextftc.units.radians
import dev.nextftc.units.rpm
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign
import org.firstinspires.ftc.teamcode.ControlHub

enum class BallType { POLLEN, NECTAR }

// distanceIn: turret axis to HIVE scoring point (inches); rpm: flywheel motor shaft; hood: unwrapped hood servo degrees
data class Shot(val distanceIn: Double, val rpm: Double, val hood: Double)

@Configurable
class Shooter : Mechanism {
    companion object {
        const val RADIANS_PER_COUNT = 2 * PI / 764 // motor encoder, so RPM is at the motor shaft
        const val MAX_RPM = 2100.0
        const val RPM_STEP = 50.0
        const val HOOD_STEP = 10.0 // degrees

        // Hood travel in unwrapped servo-feedback degrees; spans more than one turn (e.g. 40..420). MEASUREMENT
        // REQUIRED: NaN keeps the hood still and blocks shots. Keep both HOOD_MARGIN_DEG or more inside the hard stops.
        // Until then "Hood deg" counts from the boot reading, so turn the hood by hand from low to high to measure.
        const val HOOD_MIN_DEG = Double.NaN
        const val HOOD_MAX_DEG = Double.NaN
        val HOOD_CONFIGURED = !HOOD_MIN_DEG.isNaN() && !HOOD_MAX_DEG.isNaN()
        const val HOOD_MARGIN_DEG = 10.0 // beyond min/max by this much the hood faults and stops
        const val HOOD_MAX_STEP_DEG = 90.0 // feedback change per loop above this is treated as lost tracking
        const val HOOD_STALL_S = 0.5 // powered but not 1° further the right way for this long: fault
        // MEASUREMENT REQUIRED: must exceed how far the hood coasts after its power reverses (overshoot)
        const val HOOD_WRONG_WAY_DEG = 10.0 // moved this far against its power: fault
        @JvmField var hoodReversed = false // CALIBRATION REQUIRED: set so positive power raises the feedback angle
        @JvmField var hoodGains = PIDCoefficients(0.0) // CALIBRATION REQUIRED: servo power per degree of error
        @JvmField var hoodKs = 0.0 // CALIBRATION REQUIRED: least power that moves the hood
        @JvmField var hoodToleranceDeg = 3.0
        @JvmField var gains = MotorVelocityConstants()
        @JvmField var railPollen = 0.0 // servo position 0..1
        @JvmField var railNectar = 0.0 // servo position 0..1
        @JvmField var rpmTolerance = 100.0 // shots wait until measured RPM is this close to the target
        @JvmField var shotDipRpm = 300.0 // CALIBRATION REQUIRED: a drop this far below target means a ball went through

        // MEASUREMENT REQUIRED: one Shot per measured distance, increasing distance. Beyond either end the end entry is
        // used; an empty table blocks firing.
        val POLLEN_SHOTS = listOf<Shot>()
        val NECTAR_SHOTS = listOf<Shot>()
    }

    var targetRpm = 0.0
        set(rpm) {
            field = rpm.coerceIn(0.0, MAX_RPM)
        }

    // target, unwrapped servo degrees; the hood starts low, so that is where it holds until told otherwise
    var hood = HOOD_MIN_DEG
        set(deg) {
            field = deg.coerceIn(HOOD_MIN_DEG, HOOD_MAX_DEG)
        }

    // unwrapped measured angle; NaN until the first reading. Kept across OpModes like the robot, so a hood left high by
    // autonomous is still tracked in TeleOp. Only a code restart re-references it, and then the hood must be low.
    var hoodDeg = Double.NaN
        private set

    // latched until the robot code restarts: tracking can't be trusted after it
    var hoodFault = false
        private set

    private var hoodProgress = HoodProgress()

    private val hoodPid = PIDController(hoodGains)

    var ball = BallType.NECTAR

    private val motor = NextMotor(RobotController.controlHub, ControlHub.SHOOTER_MOTOR, RADIANS_PER_COUNT.radians)
    private val hoodServo = NextFeedbackCRServo(
        RobotController.controlHub,
        ControlHub.HOOD_SERVO,
        NextAnalogInput(RobotController.controlHub, ControlHub.HOOD_FEEDBACK_ANALOG),
    )
    private val rail = NextServo(RobotController.controlHub, ControlHub.RAIL_SERVO)

    init {
        gains = motor.velocityConstants
    }

    // coast at 0 instead of letting the pid brake the flywheel with reverse power
    val run = infinite {
        if (targetRpm == 0.0) motor.throttle = 0.0 else motor.setVelocitySetpoint(targetRpm.rpm)
        val error = hood - hoodDeg
        var power = if (!HOOD_CONFIGURED || hoodFault || abs(error) <= hoodToleranceDeg) 0.0
        else (hoodPid.calculate(reference = hood, measured = hoodDeg) + hoodKs * sign(error)).coerceIn(-1.0, 1.0)
        // a dead feedback wire or wrong hoodReversed would otherwise drive the hood into its hard stop
        val progress = hoodCheck(hoodProgress, hoodDeg, power, System.nanoTime())
        if (progress == null) {
            hoodFault = true
            power = 0.0
        } else {
            hoodProgress = progress
        }
        if (power == 0.0) hoodPid.reset() // no stale error or integral when it restarts
        hoodServo.power = if (hoodReversed) -power else power
        rail.position = (if (ball == BallType.NECTAR) railNectar else railPollen).coerceIn(0.0, 1.0)
    }.setEnd {
        motor.throttle = 0.0
        hoodServo.power = 0.0
    }

    // targetRpm outlives OpModes (one shared robot), so every OpMode starts with the flywheel at 0
    override val defaultCommand = instant {
        targetRpm = 0.0
        hoodProgress = HoodProgress()
    }.then(run)

    val atSpeed get() = targetRpm > 0 && abs(rpm - targetRpm) <= rpmTolerance

    val hoodAligned get() = HOOD_CONFIGURED && !hoodFault && abs(hood - hoodDeg) <= hoodToleranceDeg

    val ready get() = atSpeed && hoodAligned

    // never below rpmTolerance, so a flywheel that is at speed can't count as a shot
    val dipped get() = rpm < targetRpm - maxOf(shotDipRpm, rpmTolerance)

    private val rpm get() = motor.encoderVelocity.into(RotationsPerMinute)

    // Sets RPM and hood for `ball` at distanceIn; false (setpoints unchanged) for an unknown distance or unusable
    // table.
    fun aimFor(distanceIn: Double): Boolean {
        val shot = interpolate(if (ball == BallType.POLLEN) POLLEN_SHOTS else NECTAR_SHOTS, distanceIn) ?: return false
        targetRpm = shot.rpm
        hood = shot.hood
        return true
    }

    // these only change setpoints, so they don't require the shooter and can't interrupt run
    fun stepRpm(rpm: Double) = Commands.instant { targetRpm += rpm }

    fun stepHood(deg: Double) = Commands.instant { hood += deg }

    val switchBall = Commands.instant { ball = if (ball == BallType.POLLEN) BallType.NECTAR else BallType.POLLEN }

    override fun periodic() {
        val rawDeg = hoodServo.angle.into(Degrees)
        if (!hoodFault) {
            val next = hoodTrack(hoodDeg, rawDeg, HOOD_MIN_DEG, HOOD_MAX_DEG)
            hoodFault = next == null
            hoodDeg = next ?: hoodDeg
        }
        Telemetry.log("Shooter rpm", rpm)
        Telemetry.log("Shooter target rpm", targetRpm)
        Telemetry.log("Hood deg", hoodDeg)
        Telemetry.log("Hood target deg", hood)
        Telemetry.log("Hood raw deg", rawDeg)
        Telemetry.log("Hood fault", hoodFault)
        Telemetry.log("Rail ball", ball)
    }
}

// Linear interpolation between the two entries around distanceIn; outside the table, the nearest end entry (team,
// 2026-11-12). Null for NaN, an empty table, or distances that are not strictly increasing.
fun interpolate(table: List<Shot>, distanceIn: Double): Shot? {
    val pairs = table.zipWithNext()
    if (table.isEmpty() || distanceIn.isNaN() || pairs.any { (a, b) -> b.distanceIn <= a.distanceIn }) return null
    val d = distanceIn.coerceIn(table.first().distanceIn, table.last().distanceIn)
    val (a, b) = pairs.firstOrNull { (_, b) -> d <= b.distanceIn } ?: return table.single()
    val t = (d - a.distanceIn) / (b.distanceIn - a.distanceIn)
    return Shot(d, a.rpm + t * (b.rpm - a.rpm), a.hood + t * (b.hood - a.hood))
}

// Lowest angle >= lowDeg with this 0..360 feedback reading: the hood starts near its low end (team, 2026-11-13).
fun hoodStartDeg(rawDeg: Double, lowDeg: Double) = lowDeg + (rawDeg - lowDeg).mod(360.0)

// Unwrapped angle after a new 0..360 reading, taking the shorter way round; null if that is more than maxStepDeg.
fun unwrapDeg(unwrappedDeg: Double, rawDeg: Double, maxStepDeg: Double): Double? {
    val step = (rawDeg - unwrappedDeg + 180).mod(360.0) - 180
    return if (abs(step) > maxStepDeg) null else unwrappedDeg + step
}

// Next unwrapped hood angle from a 0..360 reading, or null when tracking is lost or the hood is more than
// HOOD_MARGIN_DEG outside minDeg..maxDeg. NaN min/max (unconfigured): starts at the raw reading, no range check.
fun hoodTrack(prevDeg: Double, rawDeg: Double, minDeg: Double, maxDeg: Double): Double? {
    val configured = !minDeg.isNaN() && !maxDeg.isNaN()
    val next = when {
        !prevDeg.isNaN() -> unwrapDeg(prevDeg, rawDeg, Shooter.HOOD_MAX_STEP_DEG) ?: return null
        configured -> hoodStartDeg(rawDeg, minDeg - Shooter.HOOD_MARGIN_DEG)
        else -> rawDeg
    }
    val outside = configured && next !in minDeg - Shooter.HOOD_MARGIN_DEG..maxDeg + Shooter.HOOD_MARGIN_DEG
    return if (outside) null else next
}

// Where (deg) and when (ns) the hood last moved 1° the way it was powered, and that way (sign of power).
data class HoodProgress(val fromDeg: Double = Double.NaN, val ns: Long = 0L, val sign: Double = 0.0)

// Progress after one loop at deg with power, or null when the hood must stop: it moved HOOD_WRONG_WAY_DEG against
// its power, or HOOD_STALL_S passed without 1° the right way. No power or a new direction restarts the check.
fun hoodCheck(progress: HoodProgress, deg: Double, power: Double, ns: Long): HoodProgress? {
    val moved = (deg - progress.fromDeg) * sign(power)
    val restart = power == 0.0 || sign(power) != progress.sign || moved >= 1.0
    return when {
        restart -> HoodProgress(deg, ns, sign(power))
        moved <= -Shooter.HOOD_WRONG_WAY_DEG || ns - progress.ns > Shooter.HOOD_STALL_S * 1e9 -> null
        else -> progress
    }
}
