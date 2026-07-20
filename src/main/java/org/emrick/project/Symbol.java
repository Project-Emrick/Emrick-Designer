package org.emrick.project;

public class Symbol {
    
    public static final String PICCOLO         = "P";
    public static final String ALTO_SAX        = "A";
    public static final String TENOR_SAX       = "S";
    public static final String CLARINET        = "C";
    public static final String TROMBONE        = "R";
    public static final String TRUMPET         = "T";
    public static final String DRUM_MAJOR      = "F";
    public static final String DRUM_MAJOR_MACE = "F";
    public static final String MELLOPHONE      = "M";
    public static final String BARITONE        = "E";
    public static final String BIG_TEN_FLAG    = "&";
    public static final String TOOBAH          = "U";
    public static final String SNARE           = "+";
    public static final String TENOR           = "n";
    public static final String BASS            = "O";
    public static final String CYMBAL          = "Y";
    public static final String BBD_CREW        = "$";
    public static final String BBD_DRUM        = "@";
    public static final String GOLDEN_SILK     = "^";

    public static int getIdPrefix(String section) {
        return switch (section) {
            case PICCOLO      -> 10000;
            case ALTO_SAX     -> 11000;
            case TENOR_SAX    -> 12000;
            case CLARINET     -> 13000;
            case TROMBONE     -> 14000;
            case TRUMPET      -> 15000;
            case DRUM_MAJOR   -> 16000;
            case MELLOPHONE   -> 17000;
            case BARITONE     -> 18000;
            case BIG_TEN_FLAG -> 19000;
            case TOOBAH       -> 20000;
            case SNARE        -> 21000;
            case TENOR        -> 22000;
            case BASS         -> 23000;
            case CYMBAL       -> 24000;
            case BBD_CREW     -> 25000;
            case BBD_DRUM     -> 26000;
            case GOLDEN_SILK  -> 27000;
            default -> 0;
        };
    }

    public static boolean isLeftOnly(String section, int label) {
        if (section.equals(GOLDEN_SILK)) {
            return true;
        }

        // assuming there are two drum majors
        // if not, change `label >= 3` accordingly
        if (section.equals(DRUM_MAJOR_MACE) && label >= 3) {
            return true;
        }

        return false;
    }

    public static boolean isLargeStrip(String section, int label) {
        if (section.equals(TOOBAH) || section.equals(BBD_DRUM) ||
            section.equals(BASS)   || section.equals(GOLDEN_SILK)) {
            return true;
        }
        
        // assuming there are two drum majors
        // if not, change `label >= 3` accordingly
        if (section.equals(DRUM_MAJOR_MACE) && label >= 3) {
            return true;
        }

        return false;
    }

}
