package org.firstinspires.ftc.teamcode.mechanisms

import dev.nextftc.hardware.webcams.NextLimelight
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.Telemetry

enum class Alliance { RED, BLUE }

enum class HiveState {
    AUDIENCE,
    COUNTER,
    NA
}

// HIVE state of the alliance's own HIVE
class Limelight(private val alliance: () -> Alliance) : Mechanism {
    companion object {
        const val PIPELINE = 0
        val RED_A_TAGS = 34..37
        val RED_B_TAGS = 30..33
        val BLUE_A_TAGS = 38..41
        val BLUE_B_TAGS = 42..45
    }

    private val limelight = NextLimelight("limelight")

    var hiveState = HiveState.NA
        private set

    override val defaultCommand = instant {
        limelight.startReading(PIPELINE)
    }.then(infinite {
        // the SDK getter is nullable, unlike NextLimelight.latestResult
        val result = limelight.camera.latestResult
        val tags = if (result != null && result.isValid) result.fiducialResults else emptyList()
        // the field is symmetric under a 180° turn: blue's far CELL (B) is where red's audience CELL (A) is, tags in the
        // same order, so states are named as red sees them
        val (aTags, bTags) = if (alliance() == Alliance.RED) RED_A_TAGS to RED_B_TAGS else BLUE_B_TAGS to BLUE_A_TAGS
        val a = tags.filter { it.fiducialId in aTags }.sortedBy { it.targetXPixels }
        val b = tags.filter { it.fiducialId in bTags }.sortedBy { it.targetXPixels }
        hiveState = when {
            a.size >= 2 -> if (a[0].fiducialId < a[1].fiducialId) HiveState.AUDIENCE else HiveState.COUNTER
            b.size >= 2 -> if (b[0].fiducialId < b[1].fiducialId) HiveState.COUNTER else HiveState.AUDIENCE
            else -> HiveState.NA
        }
    })

    override fun periodic() {
        Telemetry.log("Hive state", hiveState)
    }
}
