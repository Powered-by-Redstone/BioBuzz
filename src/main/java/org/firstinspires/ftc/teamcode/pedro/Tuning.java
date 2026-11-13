package org.firstinspires.ftc.teamcode.pedro;

import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.hardwareMap;

import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.pedropathing.tuning.autotune.Procedure;
import com.pedropathing.tuning.autotune.Tuner;
import com.pedropathing.tuning.autotune.TunerRegistrar;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.pedro.procedures.*;

public class Tuning{
    @Tuner(name="mecanumTuner")
    public static Procedure mecanumTuner() {
        return new MecanumTuner();
    }
    @Tuner(name="pinpointTuner")
    public static Procedure pinpointTuner() {
        return new PinpointTuner();
    }

    @Tuner(name="foresightTuner")
    public static Procedure foresightTuner() {
        return new ForesightTuner((hardwareMap) -> new PinpointLocalizer(hardwareMap, Constants.localizerConfig), (hardwareMap) -> new Mecanum(hardwareMap, Constants.driveConfig));
    }

    @Tuner(name="driveTest")
    public static Procedure driveTest() {
        return new Tests(hardwareMap -> new Mecanum(hardwareMap, Constants.driveConfig), null, null);
    }

    @Tuner(name="localizationTest")
    public static Procedure localizationTest() {
        return new Tests(hardwareMap -> new Mecanum(hardwareMap, Constants.driveConfig), (hardwareMap -> new PinpointLocalizer(hardwareMap, Constants.localizerConfig)), null);
    }
}
