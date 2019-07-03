package mememe.hkofflinemap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorListener;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.LocationProvider;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;
import mememe.hkofflinemap.Layer.GPSMarker;
import mememe.hkofflinemap.MapStyle.ElevateStyle;
import mememe.hkofflinemap.Util.FileUtil;
import mememe.hkofflinemap.Util.SharedPreference;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Main extends AppCompatActivity implements SensorEventListener, OnLocationUpdatedListener, XmlRenderThemeMenuCallback, SharedPreferences.OnSharedPreferenceChangeListener {
    static final int REQUEST_PERMISSIONS = 100;
    static final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private static final int LOCATION_PERMISSION_ID = 1001;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();

    /*
    Map Display Setting
     */
    private boolean firstLocation = true;

    /*
    Sensor related
     */
    float[] mGravity;
    float[] mGeomagnetic;
    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;

    /*
    Setup map related
     */
    @BindView(R.id.mainMapView)
    public MapView mapView;
    public TileCache tileCache;
    public TileRendererLayer tileRendererLayer;
    public XmlRenderThemeStyleMenu renderThemeStyleMenu;
    protected SharedPreferences sharedPreferences;
    private File mapfolder;
    private File mapfile;
    /*
    GPS service related
     */
    GPSMarker GPSmarker;
    GPSMarker viewMarker;
    LocationProvider provider;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mapContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @BindView(R.id.map_container)
    RelativeLayout mapContainer;
    @BindView(R.id.fullscreen_content_controls)
    public View mControlsView;
    @BindView(R.id.notification_bar)
    TextView notificationBar;

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createSharedPreferences();

        ButterKnife.bind(this);
        AndroidGraphicFactory.createInstance(this.getApplication());

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mVisible = true;

        setupMap();
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    private void copyMapToInternal() {
        File file = getMapFile();

        if (!file.exists()) {
            if (mapfolder.exists() && mapfolder.listFiles().length > 0) {
                for (File child : mapfolder.listFiles()) child.delete();
            }

            try {
                FileUtil.createFileFromInputStream(getAssets().open("hkmap.map"), file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File getMapFile() {
        if (mapfolder == null || mapfile == null) {

            int verCode = 0;
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                verCode = pInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            mapfolder = new File(getFilesDir(), "maps");
            mapfile = new File(mapfolder, "hkmap" + verCode + ".map");
        }

        return mapfile;
    }

    private void setupMap() {
        copyMapToInternal();

        mapView.setClickable(true);
        mapView.getMapScaleBar().setVisible(true);
        mapView.setBuiltInZoomControls(true);
        mapView.setZoomLevelMin((byte) 0);
        mapView.setZoomLevelMax((byte) 20);

        tileCache = AndroidUtil.createTileCache(this, "mapcache", mapView.getModel().displayModel.getTileSize(), 1f, mapView.getModel().frameBufferModel.getOverdrawFactor());

        MapDataStore mapDataStore = new MapFile(mapfile);
        tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setXmlRenderTheme(new ElevateStyle(this));

        Bitmap bitmap = new AndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.gps_fixed));
        GPSmarker = new GPSMarker(mapDataStore.startPosition(), bitmap, 0, 0);

        Bitmap view_bitmap = new AndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.location_view));
        viewMarker = new GPSMarker(mapDataStore.startPosition(), view_bitmap, 0, 0);

        mapView.getLayerManager().getLayers().add(tileRendererLayer);
        mapView.getLayerManager().getLayers().add(GPSmarker);
//        mapView.getLayerManager().getLayers().add(viewMarker);
        mapView.setCenter(mapDataStore.startPosition());
        mapView.setZoomLevel((byte) 16);
    }

    @Override
    protected void onDestroy() {
        this.mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        askForPermission();

        mHideHandler.post(mHideRunnable);

        startLocation();

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocation();
        mSensorManager.unregisterListener(this);
    }

    private void updatePosition(Location location) {
        Log.e("Location", "Accuracy: " + location.getAccuracy());
        if(!location.hasAccuracy()){
            notificationMsg("No fix. GPS not accurate.");
        }else if(location.getAccuracy() > 20){
            notificationMsg("GPS not accurate. ±" + location.getAccuracy() + "m");
        }

        LatLong position = new LatLong(location.getLatitude(), location.getLongitude());

        GPSmarker.setLatLong(position);
        viewMarker.setLatLong(position);

        if (firstLocation) {
            mapView.setCenter(position);
            firstLocation = false;
        } else if (SharedPreference.getMapCentreToGps(Main.this)) {
            mapView.setCenter(position);
        }
    }

    private void startLocation() {

        provider = new LocationGooglePlayServicesProvider();

        SmartLocation smartLocation = new SmartLocation.Builder(this).logging(true).build();
        LocationParams locationParams = new LocationParams.Builder().setAccuracy(LocationAccuracy.HIGH).setDistance(0).setInterval(1000).build();

        smartLocation.location(provider).config(locationParams).continuous().start(this);
//        smartLocation.activity().start(this);
    }

    private void stopLocation() {
        SmartLocation.with(this).location().stop();

        SmartLocation.with(this).activity().stop();

        SmartLocation.with(this).geofencing().stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                        }
                    }
                }
                return;
            }
        }
    }

    @OnClick(R.id.btn_gps)
    public void centerMap() {
        Location location = SmartLocation.with(this).location().getLastLocation();

        firstLocation = true;
        updatePosition(location);
    }

    @OnLongClick(R.id.btn_gps)
    public boolean toggleViewAngle() {
        try {
            if (mapView.getLayerManager().getLayers().contains(viewMarker)) {
                mapView.getLayerManager().getLayers().remove(viewMarker);
            } else {
                if(accelerometer == null || magnetometer == null){
                    notificationMsg("No Orientation sensor.");
                }else {
                    mapView.getLayerManager().getLayers().add(viewMarker);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mapView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public Set<String> getCategories(XmlRenderThemeStyleMenu menuStyle) {
        renderThemeStyleMenu = menuStyle;
        String id = sharedPreferences.getString(renderThemeStyleMenu.getId(),
                renderThemeStyleMenu.getDefaultValue());

//        XmlRenderThemeStyleLayer baseLayer = this.renderThemeStyleMenu.getLayer(id);
        XmlRenderThemeStyleLayer baseLayer = this.renderThemeStyleMenu.getLayer("elv-hiking");
        if (baseLayer == null) {
            return null;
        }
        Set<String> result = baseLayer.getCategories();

        // add the categories from overlays that are enabled
        for (XmlRenderThemeStyleLayer overlay : baseLayer.getOverlays()) {
            if (this.sharedPreferences.getBoolean(overlay.getId(), overlay.isEnabled())) {
                result.addAll(overlay.getCategories());
            }
        }

        return result;
    }

    protected void createSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // problem that the first call to getAll() returns nothing, apparently the
        // following two calls have to be made to read all the values correctly
        // http://stackoverflow.com/questions/9310479/how-to-iterate-through-all-keys-of-shared-preferences
        sharedPreferences.edit().clear();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        AndroidUtil.restartActivity(this);
    }

    @TargetApi(23)
    private void askForPermission() {
        if (!hasPermissions(PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }

    @TargetApi(23)
    public boolean hasPermissions(String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onLocationUpdated(Location location) {
        updatePosition(location);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {

                // orientation contains azimut, pitch and roll
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                float azimut = orientation[0];
                float rotation = azimut * 360 / (2 * 3.14159f);

                viewMarker.setBearing((int) rotation);
            }
        }

    }

    private void notificationMsg(String message) {
        if (notificationBar != null) {
            notificationBar.setText(message);

            showView(notificationBar);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideView(notificationBar);
                        }
                    });
                }
            }, 3000);
        }
    }

    private void showView(View v){
        if(v != null){
            ViewCompat.animate(v).alpha(1).setListener(new ViewPropertyAnimatorListener() {
                @Override
                public void onAnimationStart(View view) {
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(View view) {
                }

                @Override
                public void onAnimationCancel(View view) {
                }
            }).setDuration(500).start();
        }
    }

    private void hideView(View v){
        if(v != null){
            ViewCompat.animate(v).alpha(0).setListener(new ViewPropertyAnimatorListener() {
                @Override
                public void onAnimationStart(View view) {
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(View view) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(View view) {
                    view.setVisibility(View.GONE);
                }
            }).setDuration(500).start();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
