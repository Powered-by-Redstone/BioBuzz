package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import com.pedropathing.ivy.Command
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.hardware.actuators.NextServo
import dev.nextftc.hardware.sensors.NextColorDistanceSensor
import dev.nextftc.hardware.sensors.colors.ColorProfile
import dev.nextftc.hardware.sensors.colors.ColorSpace
import dev.nextftc.hardware.sensors.colors.NextColor
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.coroutine.command
import dev.nextftc.robot.Telemetry
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit

@Configurable
class Gate : Mechanism {
    companion object {
        const val SERVO_PORT = 4
        const val COLOR_SENSOR_BUS = 2
        @JvmField var OPEN_POS = 0.35
        @JvmField var HOLD_POS = 0.5
        @JvmField var CLOSE_POS = 0.55
        // CALIBRATION REQUIRED: raise until the brightest ball's largest channel reads about 200 of 255.
        @JvmField var COLOR_GAIN = 20.0

        // CALIBRATION REQUIRED: textbook hues (degrees), not measured on this sensor. Brightness is ignored.
        private val BALL_TOLERANCE = NextColor.hsv(25f, 0.4f, 1f)
        val RED = ColorProfile(ColorSpace.HSV, NextColor.hsv(0f, 0.6f, 1f), BALL_TOLERANCE)
        val YELLOW = ColorProfile(ColorSpace.HSV, NextColor.hsv(60f, 0.6f, 1f), BALL_TOLERANCE)
        val BLUE = ColorProfile(ColorSpace.HSV, NextColor.hsv(220f, 0.6f, 1f), BALL_TOLERANCE)
    }

    private val servo = NextServo(RobotController.controlHub, SERVO_PORT)

    private val colorSensor = NextColorDistanceSensor("sensor_color", hasDistance = true)

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

    override fun periodic() {
        colorSensor.gain = COLOR_GAIN.toFloat()
        colorSensor.update()
        Telemetry.log("Gate position", servo.position)
        Telemetry.log("Gate sensor output", colorSensor.debug())
    }
}
