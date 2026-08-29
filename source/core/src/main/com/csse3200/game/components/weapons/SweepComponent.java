package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;

public class SweepComponent extends Component {
    private final float duration;
    private final float startAngleDeg;
    private final float endAngleDeg;
    private final float radius;
    private float elapsed;

    public SweepComponent(float duration, float startAngleDeg, float endAngleDeg, float radius) {
        if (duration <= 0f) {
            throw new IllegalArgumentException("duration must be > 0");
        }
        if (radius < 0f) {
            throw new IllegalArgumentException("radius must be >= 0");
        }
        this.duration = duration;
        this.startAngleDeg = startAngleDeg;
        this.endAngleDeg = endAngleDeg;
        this.radius = radius;

    }

    @Override
    public void update() {

    }
}
