package mememe.hkofflinemap.Util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Paul Wong on 17/04/25.
 */

public class SharedPreference {
    private final static String MAP_SETTING = "Map Setting";

    private final static String MAP_CENTRE_TO_GPS = "Map center to gps";

    public static void setMapCentreToGps(Context context, Boolean center){
        try {
            SharedPreferences prefs = context.getSharedPreferences(MAP_SETTING, Context.MODE_PRIVATE);
            SharedPreferences.Editor e = prefs.edit();
            e.putBoolean(MAP_CENTRE_TO_GPS, center);
            e.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean getMapCentreToGps(Context context){
        try {
            SharedPreferences prefs = context.getSharedPreferences(MAP_SETTING, Context.MODE_PRIVATE);
            boolean center = prefs.getBoolean(MAP_CENTRE_TO_GPS, false);
            return center;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
