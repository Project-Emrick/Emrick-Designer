package org.emrick.project;

public interface ScrubBarListener {
    boolean onPlay();
    boolean onPause();
    long onScrub();
    void onSpeedChange(float playbackSpeed);
    void onTimeChange(long time);
    void onSetChange(int setIndex);
    void onVolumeChange();
}
