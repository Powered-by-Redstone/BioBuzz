package org.firstinspires.ftc.teamcode.Mechanisms

import dev.nextftc.hardware.webcams.NextLimelight
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry

val redARange = 34..37
val redBRange = 30..33
val blueARange = 38..41
val blueBRange = 42..45

enum class HiveState {
    AUDIENCE,
    COUNTER,
    NA
}

class LLTest : Mechanism {
    val limelight =  NextLimelight("limelight")

    var currentState = HiveState.NA

    override val defaultCommand = instant {
        limelight.startReading(0)
    }.then(infinite {
        val res = limelight.latestResult.fiducialResults
        val aRange = res.filter { tag-> tag.fiducialId in redARange }.sortedBy { tag -> tag.targetXPixels }
        val bRange = res.filter { tag-> tag.fiducialId in redBRange }.sortedBy { tag -> tag.targetXPixels }
        currentState = if(aRange.size >= 2) {
            if(aRange[0].fiducialId < aRange[1].fiducialId) {
                HiveState.AUDIENCE
            } else {
                HiveState.COUNTER
            }
        } else if(bRange.size >= 2) {
            if(bRange[0].fiducialId < bRange[1].fiducialId) {
                HiveState.COUNTER
            } else {
                HiveState.AUDIENCE
            }
        } else {
            HiveState.NA
        }
        Telemetry.log("Hive state",currentState)
    })
}