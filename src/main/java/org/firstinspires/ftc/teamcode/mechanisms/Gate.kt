package org.firstinspires.ftc.teamcode.mechanisms

import com.bylazar.configurables.annotations.Configurable
import com.qualcomm.robotcore.hardware.ColorRangeSensor
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.RobotController.hardwareMap
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.hardware.actuators.NextServo
import dev.nextftc.hardware.sensors.NextColorDistanceSensor
import dev.nextftc.hardware.sensors.colors.ColorProfile
import dev.nextftc.hardware.sensors.colors.NextColor
import dev.nextftc.hardware.util.LazyHardware
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry
import dev.nextftc.robot.coroutine.command
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit

@Configurable
class Gate : Mechanism {
    companion object {
        const val SERVO_PORT = 4
        const val COLOR_SENSOR_BUS = 2
        @JvmField val OPEN_POS = 0.35
        @JvmField val HOLD_POS = 0.5
        @JvmField val CLOSE_POS = 0.55

    }

    private val servo = NextServo(RobotController.controlHub, SERVO_PORT)

    private val colorSensor : ColorRangeSensor by LazyHardware {hardwareMap.get(ColorRangeSensor::class.java,"sensor_color")}

    val distance get() = colorSensor.getDistance(DistanceUnit.MM)
    val color get() = NextColor.rgb(colorSensor.red().toFloat(),colorSensor.green().toFloat(),colorSensor.blue().toFloat())

    val open = command {
        servo.position = OPEN_POS
        wait(0.3)
    }

    val close = command {
        servo.position = CLOSE_POS
        wait(0.3)
    }

    val hold = command {
        servo.position = HOLD_POS
        wait(0.3)
    }
    val intake_to_transfer = command {
        waitUntil { distance <= 60  }
        servo.position = CLOSE_POS
        wait(0.3)
    }

    override fun periodic() {
        Telemetry.log("Gate position", servo.position)
        Telemetry.log("Sensor distance", distance)
        Telemetry.log("Sensor color", color)
    }
}
