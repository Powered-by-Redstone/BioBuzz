package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextRGBIndicator
import dev.nextftc.hardware.actuators.NextRGBIndicator.Color
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import kotlin.math.PI
import kotlin.math.sin
import org.firstinspires.ftc.teamcode.ControlHub

@Configurable
class Indicator(private val state: () -> State) : Mechanism {
    enum class Pattern(val hz: Double) { SOLID(0.0), BLINK(2.0), FAST_BLINK(6.0) }

    // Robot.state() picks one, highest priority first; IDLE shows the glow instead of its color
    enum class State(val color: Color, val pattern: Pattern) {
        SHOOTING(Color.WHITE, Pattern.SOLID),
        REJECTING(Color.RED, Pattern.FAST_BLINK),
        INTAKING(Color.BLUE, Pattern.SOLID),
        NO_HIVE(Color.ORANGE, Pattern.BLINK),
        FULL(Color.GREEN, Pattern.SOLID),
        WAITING_TO_SHOOT(Color.GREEN, Pattern.BLINK),
        IDLE(Color.OFF, Pattern.SOLID),
    }

    private val light = NextRGBIndicator(RobotController.controlHub, ControlHub.INDICATOR_SERVO)

    private val seconds get() = System.nanoTime() / 1e9

    private fun show(color: Color, pattern: Pattern) =
        light.setColor(if ((seconds * pattern.hz * 2).toLong() % 2 == 0L) color else Color.OFF)

    private fun glow() {
        val mix = 0.5 + 0.25 * sin(2 * PI * seconds / 3.0) + 0.25 * sin(2 * PI * seconds / 7.3)
        light.setColor(Color.RED.position + (Color.VIOLET.position - Color.RED.position) * mix)
    }

    override val defaultCommand = infinite {
        val now = state()
        if (now == State.IDLE) glow() else show(now.color, now.pattern)
    }

    override fun periodic() {
        Telemetry.log("Robot state", state())
        Telemetry.log("Indicator position", light.position)
    }
}
