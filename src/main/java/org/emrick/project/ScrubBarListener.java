package org.emrick.project;

public interface ScrubBarListener {
    boolean onPlay();
    boolean onPause();
    long onScrub();
    void onSpeedChange(float playbackSpeed);
}
