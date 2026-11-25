package net.hihellohi.createautocrafting.client.render;

/**
 * Keyframes for the arm inside the Circuit Assembler. One cycle picks a component off the
 * tray, places it on the etched plate, then carries the finished circuit to the output chute.
 */
final class ArmAnimation {
    static final int CYCLE = 160;

    enum Held { NONE, COMPONENT, CIRCUIT }

    /** Yaw and angles are in degrees. Lean tips the lower arm forward, droop tips the upper arm down. */
    record Pose(float yaw, float lean, float droop, float clawOpen, Held held) {}

    private record Keyframe(int tick, float yaw, float lean, float droop, float clawOpen, Held held) {}

    private static final Keyframe[] KEYFRAMES = {
            new Keyframe(0, 180, 5, 15, 1, Held.NONE),
            new Keyframe(14, 180, 20, 50, 1, Held.NONE),         // down to the component tray
            new Keyframe(20, 180, 20, 50, 0, Held.COMPONENT),    // grab a component
            new Keyframe(32, 180, 5, 15, 0, Held.COMPONENT),     // lift
            new Keyframe(52, 270, 5, 15, 0, Held.COMPONENT),     // swing to the plate
            new Keyframe(64, 270, 22, 51.3f, 0, Held.COMPONENT), // lower onto the plate
            new Keyframe(70, 270, 22, 51.3f, 1, Held.NONE),      // place it
            new Keyframe(80, 270, 22, 51.3f, 1, Held.NONE),      // wait while it gets soldered
            new Keyframe(86, 270, 22, 51.3f, 0, Held.CIRCUIT),   // grab the finished circuit
            new Keyframe(98, 270, 5, 15, 0, Held.CIRCUIT),       // lift
            new Keyframe(118, 360, 5, 15, 0, Held.CIRCUIT),      // swing to the output chute
            new Keyframe(126, 360, 15, 40.2f, 0, Held.CIRCUIT),  // lower over the chute
            new Keyframe(132, 360, 15, 40.2f, 1, Held.NONE),     // drop it
            new Keyframe(144, 360, 5, 15, 1, Held.NONE),         // lift
            new Keyframe(160, 180, 5, 15, 1, Held.NONE),         // swing back to the tray
    };

    private ArmAnimation() {}

    static Pose sample(float time) {
        float t = time % CYCLE;
        for (int i = 0; i < KEYFRAMES.length - 1; i++) {
            Keyframe a = KEYFRAMES[i];
            Keyframe b = KEYFRAMES[i + 1];
            if (t >= a.tick() && t <= b.tick()) {
                float x = (t - a.tick()) / (b.tick() - a.tick());
                x = x * x * (3 - 2 * x);
                return new Pose(lerp(x, a.yaw(), b.yaw()), lerp(x, a.lean(), b.lean()),
                        lerp(x, a.droop(), b.droop()), lerp(x, a.clawOpen(), b.clawOpen()), a.held());
            }
        }
        Keyframe last = KEYFRAMES[KEYFRAMES.length - 1];
        return new Pose(last.yaw(), last.lean(), last.droop(), last.clawOpen(), last.held());
    }

    /** The component sits on the plate between being placed and the circuit being picked up. */
    static boolean showsPlacedComponent(float time) {
        float t = time % CYCLE;
        return t >= 70 && t < 86;
    }

    private static float lerp(float x, float from, float to) {
        return from + (to - from) * x;
    }
}
