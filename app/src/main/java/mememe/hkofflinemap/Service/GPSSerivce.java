package mememe.hkofflinemap.Service;

import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.yayandroid.locationmanager.base.LocationBaseService;
import com.yayandroid.locationmanager.configuration.Configurations;
import com.yayandroid.locationmanager.configuration.LocationConfiguration;

import mememe.hkofflinemap.Util.Code;

/**
 * Created by Paul Wong on 17/04/19.
 */

public class GPSSerivce extends LocationBaseService {
    public static final String ACTION_LOCATION_CHANGED = "mememe.hkofflinemap.Service.LOCATION_CHANGED";

    @Override
    public LocationConfiguration getLocationConfiguration() {
        return Configurations.silentConfiguration(false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("Current Location", "Lat: " + location.getLatitude() + "; Long: " + location.getLongitude());
        Intent intent = new Intent(ACTION_LOCATION_CHANGED);
        intent.putExtra(Code.LOCATION_UPDATE, location);
        sendBroadcast(intent);
    }

    @Override
    public void onLocationFailed(int type) {
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // calling super is required when extending from LocationBaseService
        super.onStartCommand(intent, flags, startId);

        getLocation();

        // Return type is depends on your requirements
        return START_NOT_STICKY;
    }
}
