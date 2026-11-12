package org.firstinspires.ftc.teamcode

// Where each device is plugged in. The object a port sits in names its hub; the mechanism must pass that same hub.

object ControlHub {
    const val INTAKE_MOTOR = 2
    const val TRANSFER_MOTOR = 3
    const val INDICATOR_SERVO = 0
    const val GATE_SERVO = 4
    const val GATE_COLOR_SENSOR_I2C_BUS = 2
}

object ExpansionHub {
    const val TURRET_MOTOR = 2
    const val SHOOTER_MOTOR = 3
    const val HOOD_SERVO = 0
}
