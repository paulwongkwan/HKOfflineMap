package mememe.hkofflinemap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
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
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.LocationProvider;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;
import mememe.hkofflinemap.MapStyle.ElevateStyle;
import mememe.hkofflinemap.Util.BitmapUtil;
import mememe.hkofflinemap.Util.FileUtil;
import mememe.hkofflinemap.Util.SharedPreference;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Main extends AppCompatActivity implements OnLocationUpdatedListener, XmlRenderThemeMenuCallback, SharedPreferences.OnSharedPreferenceChangeListener {
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
    private final float gpsMarkerDp = 20;

    /*
    Setup map related
     */
    @BindView(R.id.mainMapView)
    public MapView mapView;
    public TileCache tileCache;
    public TileRendererLayer tileRendererLayer;
    public XmlRenderThemeStyleMenu renderThemeStyleMenu;
    protected SharedPreferences sharedPreferences;

    /*
    GPS service related
     */
    Marker GPSmarker;
    LocationProvider provider;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mapView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @BindView(R.id.fullscreen_content_controls)
    public View mControlsView;
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

        mVisible = true;

        // Set up the user interaction to manually show or hide the system UI.
        mapView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        AndroidGraphicFactory.createInstance(this.getApplication());
        setupMap();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_ID);
            return;
        }
    }

    private void copyMapToInternal() {
        File file = new File(getFilesDir(), "hkmap.map");

        if (!file.exists()) {
            try {
                FileUtil.createFileFromInputStream(getAssets().open("hkmap.map"), file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupMap() {
        copyMapToInternal();

        mapView.setClickable(true);
        mapView.getMapScaleBar().setVisible(true);
        mapView.setBuiltInZoomControls(true);
        mapView.setZoomLevelMin((byte) 0);
        mapView.setZoomLevelMax((byte) 20);

        tileCache = AndroidUtil.createTileCache(this, "mapcache", mapView.getModel().displayModel.getTileSize(), 1f, mapView.getModel().frameBufferModel.getOverdrawFactor());

        MapDataStore mapDataStore = new MapFile(new File(getFilesDir(), "hkmap.map"));
        tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setXmlRenderTheme(new ElevateStyle(this));

        int px = BitmapUtil.convertDpToPixel(gpsMarkerDp, Main.this);
        Bitmap bitmap = BitmapUtil.decodeSampledBitmapFromResource(getResources(), R.drawable.gps_fixed, px, px);
        GPSmarker = new Marker(mapDataStore.startPosition(), bitmap, 0, 0);

        mapView.getLayerManager().getLayers().add(tileRendererLayer);
        mapView.getLayerManager().getLayers().add(GPSmarker);
        mapView.setCenter(mapDataStore.startPosition());
        mapView.setZoomLevel((byte) 12);
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

        startLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocation();

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
        if (requestCode == LOCATION_PERMISSION_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation();
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

    @Override
    public void onLocationUpdated(Location location) {
        GPSmarker.setLatLong(new LatLong(location.getLatitude(), location.getLongitude()));
        if (SharedPreference.getMapCentreToGps(Main.this))
            mapView.setCenter(new LatLong(location.getLatitude(), location.getLongitude()));
    }
}
