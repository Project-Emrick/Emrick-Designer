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
}
