plugins {
    id("dev.frozenmilk.teamcode") version "12.0.0-1.2.1"
    id("dev.frozenmilk.sinister.sloth.load") version "0.3.2"
}

ftc {
    kotlin()

    sdk {
        implementation(RobotCore)

        version = "12.0.0"

        TeamCode()
    }

    dairy {
        implementation(Sloth)
        // if you want panels too
        implementation(ftControl.fullpanels)
    }

    pedro {
        implementation(core)
        implementation(ftc)
        implementation(telemetry)
    }

    next.v2 {
        implementation(control)
        implementation(hardware)
        implementation(robot)
    }
}
