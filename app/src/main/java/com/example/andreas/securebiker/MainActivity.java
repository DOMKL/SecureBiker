package com.example.andreas.securebiker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.andreas.securebiker.Fragments.AllPreferencesFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final int REQUESTCODE_SETTINGS = 1;
    private static final String NOT = "NOTIFICATIONS";

    // Settings
    // Boolean-Flags für Settings
    private boolean soundEnabled = true;
    private boolean vibrationEnabled = true;
    private boolean alertDialogEnabled = true;
    // Anpassbarer_Geofence_Durchmesser
    private int geofenceDiameter = 150;
    // Laufzeit des AlarmDialog-Fensters/Vibrations-Alarms/
    public static final String TIME = "time";
    private int time = 6;
    // Durchmesser des Geofence-Kreis
    public static final int GEOFENCE_CIRCLE_DIAMETER = 5;
    // Vibrations-Muster für Alarm-Notification
    public static final int THREE_SECONDS = 3;
    public static final long[] vibrationThreeSeconds = {0, 1000, 1000, 2000};
    public static final int SIX_SECONDS = 6;
    public static final long[] vibrationSixSeconds = {0, 1000, 1000, 1000, 1000, 2000};
    public static final int NINE_SECONDS = 10;
    public static final long[] vibrationNineSeconds = {0, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 2000};

    // Kamerazoom
    private int cameraZoom = 16;

    private Timer timer;
    private DialogFragment newFragment;
    private GoogleApiClient googleApiClient;
    private GoogleMap mMap;
    private CameraPosition cameraPosition;
    private Location lastLocation;
    private LatLng pos;
    private LocationRequest locationRequest;
    private Marker marker;
    private ArrayList<Geofence> geofenceList;
    private AlarmBroadcastReceiver aB;
    private ArrayList<Circle> geofenceCircles;
    private ArrayList<LatLng> ltlng;
    private FileReaderTask task = null;
    private boolean alarmDialogOn = false;
    private int alarmDialogTimer = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Prüft GPS-Status
        checkGPS();

        // GUI-Gedöns
        setContentView(R.layout.activity_map);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        loadPreferences();
        task = new FileReaderTask();

        geofenceList = null;
        geofenceCircles = new ArrayList<>();

        // BroadcastReceiver-Gedöns
        aB = new AlarmBroadcastReceiver();
        IntentFilter iFilter = new IntentFilter((GeofenceIntentService.BROADCAST_ACTION));
        iFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(aB, iFilter);

        // Initalisierung des Location Request
        createLocationRequest();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        buildGoogleApiClient();

        // load preferences from the system

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                //open Settings-Activity
                runSettingsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Methode wird aufgerufen, wenn der Settings-Button gedrückt wird
     */
    private void runSettingsActivity() {
        Intent settings_intent = new Intent(this, SettingsActivity.class);
        startActivity(settings_intent);
    }

    /**
     * load preferences from the system
     */
    private void loadPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.pref_all, false);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Radius der Geofence
        geofenceDiameter = sharedPrefs.getInt(AllPreferencesFragment.KEY_FENCES_RADIUS, 50);
        // Einstellung des Alarm-Buttons (On/Off)
        alarmDialogOn = sharedPrefs.getBoolean(AllPreferencesFragment.KEY_ALARMSWITCH, true);
        //alarmtimer
        time = Integer.parseInt(sharedPrefs.getString(AllPreferencesFragment.KEY_ALARMDIALOGTIMER,"0"));
        // Vibration
        vibrationEnabled = sharedPrefs.getBoolean(AllPreferencesFragment.KEY_NOTIFI_MESSAGE_VIB,true);
        // Klingelton
        soundEnabled = sharedPrefs.getBoolean(AllPreferencesFragment.KEY_ALARMSWITCH,true);
    }

    /**
     * protected void onActivityResult(int requestCode, int resultCode, Intent data) {
     * // Check which request we're responding to
     * if (requestCode == REQUESTCODE_SETTINGS && resultCode == RESULT_OK) {
     * // Make sure the request was successful
     * Bundle extras = data.getExtras();
     * //Radius der Geofences hohlen
     * extras.get("fences_radius");
     * /**
     * Bundle extras = data.getExtras();
     * Bitmap image =(Bitmap) extras.get("data");
     * ImageView imageView = (ImageView) findViewById(R.id.imageView);
     * imageView.setImageBitmap(image);
     * <p/>
     * }
     * <p/>
     * }
     **/
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        //turn sound on/off
        if (id == R.id.nav_sound) {
            //turn vibration on/off
        } else if (id == R.id.nav_vibration) {
            //turn visual alert on/off
        } else if (id == R.id.nav_visual) {
            DrawerLayout drawer = (DrawerLayout) findViewById(id);
            /**      ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, drawer,
             R.drawable.ic_info_black_24dp, // nav menu toggle icon
             R.string.app_name, // nav drawer open - description for
             // accessibility
             R.string.app_name // nav drawer close - description for
             // accessibility
             );
             **/
        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        loadPreferences();
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Verbindet den Client mit Google Play Services
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // Beendigung aller laufenden Notifications
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(0);
        if (googleApiClient.isConnected()) {
            stopLocationUpdates();
        }
        //schließt die Verbindung zu Google Play Services
        googleApiClient.disconnect();
        if (timer != null)
            timer.cancel();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        // currentGeofences.clear();
        // Entfernt aktuelle Puffer um Gefahrenstellen
       /* for (int i = 0; i < geofenceCircles.size(); i++)
            geofenceCircles.get(i).remove();
        // Entfernt Marker zur aktuellen Position
        if (!(marker == null)) {
            marker.remove();
        }*/
        // Damit WarnDialog nicht in outState gespeichert wird und somit beim Neuaufbau der Activity nicht zwei Dialoge stehen können
        if (newFragment != null)
            newFragment.dismiss();
        super.onSaveInstanceState(outState);
    }

    /**
     * Methode, die überprüft, ob GPS aktiviert ist
     */
    private void checkGPS() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean gpsEnabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    /**
     * Methode zur Erzeugung der Google Play Service Api Instanz
     */
    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API) //Integration des Location Service
                .build();
    }

    /**
     * Methode zur Erzeugung eines Location Request
     */
    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(20000); // Intervall, in dem Updates zur Location empfangen werden sollen
        locationRequest.setFastestInterval(10000); // Zur Vermeidung von Komplikationen in Zusammenhang mit anderen Apps, die parallel in noch schnellerem Intervall LocationUpdates empfangen: Definition eines Intervall-Oberlimits
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Höchst mögliche Präzision der zu ermittelnden Location
    }

    /**
     * Methode zum Start des Empfangs von Location Updates
     */
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    /**
     * Methode zum Stoppen des Empfangs von Location Updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    /**
     * Methode zur Aktualisierung der GUI/Map
     */
    private void updateUI() {
        pos = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        marker.setPosition(pos);
        mMap.animateCamera(CameraUpdateFactory.newLatLng(pos));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onConnected(Bundle bundle) {
        lastLocation = LocationServices.FusedLocationApi.getLastLocation((googleApiClient));
        if (lastLocation != null) {
            // Auslesen der Koordinaten aus lastLocation
            pos = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            marker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Here I am!"));
            // Defintion von Kamera-Einstellungen
            cameraPosition = new CameraPosition.Builder()
                    .target(pos)
                    .zoom(cameraZoom)
                    .build();
            // Kamera wird auf aktuelle Position ausgerichtet
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        // Location-Updates werden gestartet
        startLocationUpdates();

        // Initalisierung und Registrierung der Geofences
        task.execute();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Die Verbindung zur Google Play Services ist unterbrochen worden
        // LocationUpdates werden ausgesetzt
        stopLocationUpdates();
        // Aufruf von connect(), um Verbindung wieder aufzubauen
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        CharSequence text = "Verbindungsaufbau gescheitert!";
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        updateUI();
    }


    /**
     *
     * Ab hier folgt Geofencing-Gedöns
     *
     */


    /**
     * Methode zur Initalisierung der Liste mit Test-Geofences

     private ArrayList<LatLng> initializeGeofences() {
     ltlng = HelperClass.getExample(this);
     return ltlng;
     }*/

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(geofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     */
    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    /**
     * Quelltext zum entfernen von Geofences
     * LocationServices.GeofencingApi.removeGeofences(
     * mGoogleApiClient,
     * // This is the same pending intent that was used in addGeofences().
     * getGeofencePendingIntent()
     **/

    private void registerGeofences() {
        LocationServices.GeofencingApi.addGeofences(
                googleApiClient,
                getGeofencingRequest(),
                getGeofencePendingIntent()
        );
    }

    /**
     * BroadcastReceiver zum Empfang von Nachrichten vom GeofenceIntentService
     */
    public class AlarmBroadcastReceiver extends WakefulBroadcastReceiver {

        public AlarmBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            buildNotification(time, soundEnabled, vibrationEnabled); // Notification-Versand
            if (newFragment == null && alertDialogEnabled)
                showAlarmDialog();
        }

        /**
         * Methode zur Darstellung des Warn-Dialogs
         */
        public void showAlarmDialog() {
            newFragment = new AlarmDialogFragment();
            try {
                newFragment.show(getSupportFragmentManager(), "alarm");
            } catch (IllegalStateException e) {
                return;
            }
            // TimerTask zur automatischen Schließung des DialogFensters
            timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    try {
                        newFragment.dismiss();
                        // Beendigung der Notification
                        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        nMgr.cancel(0);
                        // nach Ablauf des Timers schließt sich das DialogFenster automatisch
                    } catch (IllegalStateException e) {
                    } finally {
                        newFragment = null;
                    }

                }
            }, (time * 1000)); // Laufzeit des DialogFensters
        }
    }

    /**
     * Methode zur Bildung und Versand von Warn-Notification mit Alarm-Sound
     */

    public void buildNotification(int time, boolean sound, boolean vibration) {
        // Notification-Gedöns
        Uri a = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alarm_bicycle_bell);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        //Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        // Uri alarmSound = RingtoneManager.getDefaultUri(R.raw.Luft_Alarm);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder;
        mBuilder = new NotificationCompat.Builder(this).setCategory(Notification.CATEGORY_ALARM);
        if (sound)
            mBuilder.setSound(a);
        // Setting the vibration pattern
        if (vibration) {
            long[] vibrationPattern = vibrationSixSeconds;
            switch (time) {
                case THREE_SECONDS:
                    vibrationPattern = vibrationThreeSeconds;
                case NINE_SECONDS:
                    vibrationPattern = vibrationNineSeconds;
                case SIX_SECONDS:
                    vibrationPattern  = vibrationSixSeconds;
            }
            mBuilder.setVibrate(vibrationPattern);
            /*vibrationPattern[0] = 0L;
            for (int i = 1; i < vibrationPattern.length; i++) {
                vibrationPattern[i] = (long) 1000;
            }*/
        }
        mNotificationManager.notify(0, mBuilder.build());
    }

    /**
     * Eigene AsyncTask-Klasse zum Import von Geofence-Locations
     */
    public class FileReaderTask extends AsyncTask<Void, CircleOptions, ArrayList<Geofence>> {

        InputStream iS = null;
        InputStreamReader iSR = null;
        BufferedReader bR = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(ArrayList<Geofence> geofences) {
            geofenceList = geofences;
            registerGeofences();
        }

        @Override
        protected void onProgressUpdate(CircleOptions... cO) {
            // Gefahrenstelle wird auf Karte sichtbar gemacht
            Circle c = mMap.addCircle(cO[0]);
            // Gefahrenstelle wird in ArrayList aufgenommen
            geofenceCircles.add(c);
        }

        @Override
        protected ArrayList<Geofence> doInBackground(Void... params) {
            ArrayList<Geofence> geofenceList = new ArrayList<>();
            String s = "";
            try {
                // Initialisierung der Lese-Ströme zum Import der Gefahrenstellen
                iS = getResources().openRawResource(R.raw.examplepoints);
                iSR = new InputStreamReader(iS);
                bR = new BufferedReader(iSR);
                int i = 0; //RequestId für Geofences
                while ((s = bR.readLine()) != null) {
                    LatLng l = stringToLatLng(s);
                    CircleOptions c = getCircleOptions(l);
                    Geofence a = getGeofence(i, l);
                    geofenceList.add(a);
                    i++;
                    publishProgress(c); // Übergabe des Circle-Objektes an onProgressUpdate-Methode
                }
            } catch (IOException e) {
                //TODO TBD
            }
            return geofenceList;
        }


        /**
         * Konvertierung eines Strings mit LatLng-Koordinaten in ein LatLng-Objekt
         *
         * @param s
         * @return
         */
        private LatLng stringToLatLng(String s) {
            String[] temp = s.split(";");
            // int num = Integer.parseInt(temp[0]);
            double lat = Double.parseDouble(temp[1]);
            double lon = Double.parseDouble(temp[2]);
            LatLng l = new LatLng(lat, lon);
            return l;
        }

        /**
         * Erzeugung eines CircleOptions-Objektes aus LatLng-Objekt
         *
         * @param l
         * @return
         */
        private CircleOptions getCircleOptions(LatLng l) {
            return new CircleOptions()
                    .radius(GEOFENCE_CIRCLE_DIAMETER)
                    .center(l)
                    .fillColor(Color.argb(100, 0, 0, 255))
                    .strokeWidth(0.1f);
        }

        /**
         * Erzeugung eines Geofence-Objekt aus LatLng-Objekt sowie int-Wert als ID
         *
         * @param i
         * @param l
         * @return
         */
        private Geofence getGeofence(int i, LatLng l) {
            return new Geofence.Builder().setCircularRegion(l.latitude, l.longitude, geofenceDiameter)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setRequestId(Integer.toString(i)).build();
        }
    }


}
