import dev.frozenmilk.easyautolibraries.EasyAutoDependency

plugins {
    id("dev.frozenmilk.teamcode") version "12.0.0-1.2.2"
    id("dev.frozenmilk.sinister.sloth.load") version "0.3.2"
}

ftc {
    kotlin()

    sdk {
        implementation(RobotCore)

        TeamCode()
    }

    dairy {
        implementation(Sloth)
        // if you want panels too
        implementation(ftControl.fullpanels("0.3.2+1.0.13"))
    }

//    pedro {
//        implementation(core)
//        implementation(ftc)
//        implementation(telemetry)
//    }

    next.v2 {
        implementation(control("0.2.1"))
        implementation(hardware("0.2.1"))
        implementation(robot("0.2.1"))
    }
}

dependencies {
    implementation("com.pedropathing:revhub:3.0.1")
    implementation("com.pedropathing:tuning:1.0.2")
}
