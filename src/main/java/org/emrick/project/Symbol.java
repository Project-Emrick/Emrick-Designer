package org.emrick.project;

import java.util.Collections;
import java.util.List;

public class Symbol {
    
    public static final String PICCOLO         = "P";
    public static final String ALTO_SAX        = "A";
    public static final String TENOR_SAX       = "S";
    public static final String CLARINET        = "C";
    public static final String TROMBONE        = "R";
    public static final String TRUMPET         = "T";
    public static final String MELLOPHONE      = "M";
    public static final String BARITONE        = "E";
    public static final String BIG_TEN_FLAG    = "&";
    public static final String TOOBAH          = "U";
    public static final String SNARE           = "+";
    public static final String TENOR           = "n";
    public static final String QUAD            = "n";
    public static final String BASS            = "O";
    public static final String CYMBAL          = "Y";
    public static final String BBD_DRUM        = "@";
    public static final String BBD_CREW        = "$";
    public static final String GOLDEN_SILK     = "^";
    public static final String DRUM_MAJOR      = "F";
    public static final String DRUM_MAJOR_MACE = "F";

    // GDS will appear separately as last entries in the CSV file
    public static final List<String> CSV_ORDER = Collections.unmodifiableList(List.of(
        PICCOLO,
        ALTO_SAX,
        TENOR_SAX,
        CLARINET,
        TROMBONE,
        TRUMPET,
        MELLOPHONE,
        BARITONE,
        BIG_TEN_FLAG,
        TOOBAH,
        SNARE,
        QUAD,
        BASS,
        CYMBAL,
        BBD_DRUM,
        BBD_CREW,
        GOLDEN_SILK,
        DRUM_MAJOR
    ));

}
