package org.emrick.project.dev;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

/**
 * Details a receiver reports in reply to the "info" serial command:
 * {@code @INFO {"type":"receiver","fw":"abc1234","id":12,"label":"T10L",...}}
 */
public class UnitInfo {
    private static final Gson GSON = new Gson();

    public String type;
    public String fw;
    @SerializedName("fw_date")
    public String fwDate;
    public int id;
    public String label;
    public String position;
    public int leds;
    public long token;
    public int packets;
    @SerializedName("verification_color")
    public String verificationColor;
    @SerializedName("battery_v")
    public double batteryVolts;
    @SerializedName("battery_pct")
    public double batteryPercent;
    public String state;
    public boolean debug;
    public String mac;

    /**
     * @param line a line starting with "@INFO "
     * @return the parsed info, or null if the line isn't valid
     */
    public static UnitInfo parse(String line) {
        if (line == null || !line.startsWith(UnitPort.INFO_PREFIX)) {
            return null;
        }
        try {
            return GSON.fromJson(line.substring(UnitPort.INFO_PREFIX.length()), UnitInfo.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    /** @return the firmware commit without a "-dirty" suffix, for comparing against a release */
    public String fwCommit() {
        if (fw == null) {
            return "";
        }
        return fw.endsWith("-dirty") ? fw.substring(0, fw.length() - 6) : fw;
    }
}
