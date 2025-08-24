package org.emrick.project;

/**
 * Interface for receiving notifications about timeline events
 */
public interface TimelineListener {
    /**
     * Called when the timeline is scrubbed to a specific count
     * @param count The count position that was scrubbed to
     */
    void onTimelineScrub(double count);
}
