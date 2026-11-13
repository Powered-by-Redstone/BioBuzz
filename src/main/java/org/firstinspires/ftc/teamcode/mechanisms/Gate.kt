package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextServo
import dev.nextftc.hardware.sensors.NextColorDistanceSensor
import dev.nextftc.hardware.sensors.colors.ColorProfile
import dev.nextftc.hardware.sensors.colors.ColorSpace
import dev.nextftc.hardware.sensors.colors.NextColor
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import dev.nextftc.robot.coroutine.command
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.teamcode.ControlHub
import org.firstinspires.ftc.teamcode.ExpansionHub

@Configurable
class Gate : Mechanism {
    companion object {
        @JvmField var OPEN_POS = 0.17
        @JvmField var HOLD_POS = 0.32
        @JvmField var CLOSE_POS = 0.58
        // CALIBRATION REQUIRED: raise until the brightest ball's largest channel reads about 200 of 255.
        @JvmField var COLOR_GAIN = 15.0
        @JvmField var BALL_MM = 14.0 // a ball is at the gate when closer than this
        // extra wait after closing (on top of close's 0.3 s) for the ball to enter the transfer
        @JvmField var transferS = 0.0
        // collect rejects a held ball whose color matches none of the profiles by then
        @JvmField var colorTimeoutS = 3.0

        // CALIBRATION REQUIRED: textbook hues (degrees), not measured on this sensor. Brightness is ignored.
        private val BALL_TOLERANCE = NextColor.hsv(25f, 0.4f, 1f)
        val RED = ColorProfile(ColorSpace.HSV, NextColor.hsv(356f, 0.6f, 1f), BALL_TOLERANCE)
        val YELLOW = ColorProfile(ColorSpace.HSV, NextColor.hsv(20f, 0.6f, 1f), BALL_TOLERANCE)
        val BLUE = ColorProfile(ColorSpace.HSV, NextColor.hsv(190f, 0.6f, 1f), BALL_TOLERANCE)
    }

    private val servo = NextServo(RobotController.expansionHub, ExpansionHub.GATE_SERVO)

    private val colorSensor =
        NextColorDistanceSensor(RobotController.controlHub, ControlHub.GATE_COLOR_SENSOR_I2C_BUS, hasDistance = true)

    val open = command {
        servo.position = OPEN_POS
        wait(0.3)
    }.requiring(this)

    val close = command {
        servo.position = CLOSE_POS
        wait(0.3)
    }.requiring(this)

    val hold = command {
        servo.position = HOLD_POS
        wait(0.3)
    }.requiring(this)

    val acceptBall = command {
        await(open)
        waitUntil { colorSensor.isColorWithinDistance(YELLOW,160.0,DistanceUnit.MM) }
        await(hold)
        await(close)
    }.requiring(this)

    val hasBall get() = colorSensor.isWithinDistance(BALL_MM, DistanceUnit.MM)

    // YELLOW, RED or BLUE once a ball at the gate matches it, else null
    val color get() =
        listOf(YELLOW, RED, BLUE).firstOrNull { colorSensor.isColorWithinDistance(it, BALL_MM, DistanceUnit.MM) }

    override fun periodic() {
        colorSensor.gain = COLOR_GAIN.toFloat()
        colorSensor.update()
        Telemetry.log("Gate position", servo.position)
        Telemetry.log("Gate sensor output", colorSensor.debug())
    }
}
