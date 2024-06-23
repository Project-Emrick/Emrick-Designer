package org.emrick.project.effect;

public class GeneratedEffectLoader {
    public static StaticColorEffect generateStaticColorEffectFromEffect(Effect e) {
        return new StaticColorEffect(e.getStartTimeMSec(), e.getEndTimeMSec(), e.getStartColor(), e.getDuration(), e.getId());
    }

    public static WaveEffect generateWaveEffectFromEffect(Effect e) {
        return new WaveEffect(e.getStartTimeMSec(), e.getEndTimeMSec(), e.getStartColor(), e.getEndColor(), e.getDuration(), e.getSpeed(), e.isUpOrSide(), e.isDirection(), e.getId());
    }
}
