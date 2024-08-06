package org.emrick.project.effect;

public class GeneratedEffectLoader {
    public static StaticColorEffect generateStaticColorEffectFromEffect(Effect e) {
        return new StaticColorEffect(e.getStartTimeMSec(), e.getEndTimeMSec(), e.getStartColor(), e.getDelay(), e.getId());
    }

    public static WaveEffect generateWaveEffectFromEffect(Effect e) {
        return new WaveEffect(e.getStartTimeMSec(), e.getEndTimeMSec(), e.getStartColor(), e.getEndColor(), e.getDuration(), e.getSpeed(), e.isUpOrSide(), e.isDirection(), e.getId());
    }

    public static FadeEffect generateFadeEffectFromEffect(Effect e) {
        return new FadeEffect(e.getStartTimeMSec(), e.getEndTimeMSec(), e.getStartColor(), e.getEndColor(), e.getDuration(), e.getId());
    }

    public static AlternatingColorEffect generateAlternatingColorEffectFromEffect(Effect e) {
        return new AlternatingColorEffect(e.getStartTimeMSec(), e.getEndTimeMSec(), e.getStartColor(), e.getEndColor(), e.getDuration(), e.getSpeed(), e.getId());
    }

    public static RippleEffect generateRippleEffectFromEffect(Effect e) {
        return new RippleEffect(e.getStartTimeMSec(), e.getEndTimeMSec(), e.getStartColor(), e.getEndColor(), e.getDuration(), e.getSpeed(), e.isUpOrSide(), e.isDirection(), e.getId());
    }

    public static CircleChaseEffect generateCircleChaseEffectFromEffect(Effect e) {
        return new CircleChaseEffect(e.getStartTimeMSec(), e.getEndTimeMSec(), e.getStartColor(), e.getEndColor(), e.getDuration(), e.isDirection(), e.getAngle(), e.getSpeed(), e.getId());
    }

    public static ChaseEffect generateChaseEffectFromEffect(Effect e) {
        return new ChaseEffect(e.getStartTimeMSec(), e.getEndTimeMSec(), e.getChaseSequence(), e.getDuration(), e.isDirection(), e.getSpeed(), e.getId());
    }
}
