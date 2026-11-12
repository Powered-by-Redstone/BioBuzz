package org.firstinspires.ftc.teamcode.mechanisms

import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextRGBIndicator
import dev.nextftc.hardware.actuators.NextRGBIndicator.Color
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import kotlin.math.PI
import kotlin.math.sin

class Indicator : Mechanism {
    companion object {
        const val SERVO_PORT = 4
    }

    enum class Pattern(val hz: Double) { SOLID(0.0), BLINK(2.0), FAST_BLINK(6.0) }

    // never call setBrightness: it changes the PWM range and shifts every color
    private val light = NextRGBIndicator(RobotController.controlHub, SERVO_PORT)

    private val seconds get() = System.nanoTime() / 1e9

    // on for the first half of each blink period; 0 Hz stays on
    private fun show(color: Color, pattern: Pattern) =
        light.setColor(if ((seconds * pattern.hz * 2).toLong() % 2 == 0L) color else Color.OFF)

    // two sine waves with unrelated periods, so the hue wanders without an obvious repeat; mix stays in 0..1
    private fun glow() {
        val mix = 0.5 + 0.25 * sin(2 * PI * seconds / 3.0) + 0.25 * sin(2 * PI * seconds / 7.3)
        light.setColor(Color.RED.position + (Color.VIOLET.position - Color.RED.position) * mix)
    }

    // robot states go above the glow, highest priority first; their inputs come in as constructor lambdas from Robot
    override val defaultCommand = infinite {
        when {
            else -> glow()
        }
    }

    override fun periodic() {
        Telemetry.log("Indicator position", light.position)
    }
}
