package org.emrick.project.audio;

import java.nio.ByteBuffer;


/**
 * <a href="https://github.com/lsliwko/jorbis-oggdecoder/blob/master/src/com/jcraft/oggdecoder/OggData.java">...</a>
 */
public class OggData {
    /** The data that has been read from the OGG file */
    public ByteBuffer data;

    /** The sampling rate */
    public int rate;

    /** The number of channels in the sound file */
    public int channels;
}
