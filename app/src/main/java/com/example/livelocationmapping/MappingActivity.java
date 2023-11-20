package com.example.livelocationmapping;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.os.SystemClock.sleep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MappingActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener{

    Activity act;
    static Context context;

    static dbHelper doa;

    String mapping_mode_type = "";

    public Button startmapping, restart, completemapping;
    public ImageView clearlastpoint;
    public TextView acerage;

    SphericalUtil sphutil;
    Double area_ = 0.0;
    static boolean startedMapping = false;
    boolean one_time_centering = false;
    private static boolean checkClick = false;

    double start_latitude = 0;
    double start_longitude = 0;
    private Location lastLocation;
    private static GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest locationRequest;

    private static ArrayList<LatLng> arrayPoints = null;
    static ArrayList<LatLng> exisingarrayPoints = null;
    static List<ArrayList<LatLng>> all_polygonBlocksData = null;
    private static ArrayList<Marker> arrayMarkers = null;
    static boolean isMappingPaused = false;
    static Polyline polylineFinal = null;
    Polygon current_mapping_polygon;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!isGooglePlayServicesAvailable()) {
            Toast.makeText(this, "Google Play Services is not available", Toast.LENGTH_LONG).show();
            finish();
        } else {
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mapping_mode_type = getApplicationContext().getSharedPreferences("com.example.alltestsandroid", Context.MODE_PRIVATE).getString("mapping_mode", "Phone Gps"); //to return when using phone gps(walking around)
        mapping_mode_type = getApplicationContext().getSharedPreferences("com.example.alltestsandroid", Context.MODE_PRIVATE).getString("mapping_mode", "Manual GPS");//to return when picking points

        act = this;
        context = this;
        doa = new dbHelper(this, this);

        startmapping = (Button) findViewById(R.id.startmapping);
        restart = (Button) findViewById(R.id.restart);
        completemapping = (Button) findViewById(R.id.completemapping);
        clearlastpoint = (ImageView) findViewById(R.id.clearlastpoint);
        acerage = (TextView) findViewById(R.id.acerage);

        arrayPoints = new ArrayList<LatLng>();
        arrayMarkers=new ArrayList<Marker>();
        all_polygonBlocksData = new ArrayList<>();

        //checking permissions
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 0);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        initLocationServicesNow();

        //start mapping button
        startmapping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startMappingAllowed();

            }
        });

        //complete mapping button
        completemapping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LatLng polygon_start = null;
                LatLng polyon_end = null;

                if (arrayPoints.size() < 2) {
                    //no point picked
                } else {
                    polygon_start = arrayPoints.get(0);
                    polyon_end = arrayPoints.get(arrayPoints.size() - 1);

                    //checking distance before completing polygon
//                    boolean isPolygonOkey = countPolygonPoints();
//                    double distance_to_start = getDistanceAtoB(polygon_start.latitude, polygon_start.longitude, polyon_end.latitude, polyon_end.longitude);
//                    if(distance_to_start > 15){
//                        Toast.makeText(getApplicationContext(), "Too Far From Starting point", Toast.LENGTH_LONG).show();
//                        isPolygonOkey = false;
//                    } else {
                    boolean isPolygonOkey = countPolygonPoints();
                    if (isPolygonOkey) {
                        if (all_polygonBlocksData != null){
                            //check for overlap
//                                if(overlap){}

                            checkClick = false;
                            startedMapping = false;
                            open();

                        } else {
                            checkClick = false;
                            startedMapping = false;
                            open();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Invalid Polygon! NOT SAVED", Toast.LENGTH_LONG).show();
                    }

//                    }
                }
            }
        });

        //clear last point
        clearlastpoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(arrayMarkers.size() > 0) {
                    arrayMarkers.get(arrayMarkers.size() - 1).remove();
                    arrayMarkers.remove(arrayMarkers.size() - 1);
                    arrayPoints.remove(arrayPoints.size() - 1);
                    drawPolyline();
                    if(current_mapping_polygon !=null)
                        current_mapping_polygon.remove();

                    doa.deleteExistingCacheLastPoint();
                }

            }
        });

        //restart mapping whole polygon
        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteWarning();
            }
        });


    }

    @Override
    public void onStart() {
        super.onStart();
        if (!checkPermissions()) {
            startLocationUpdates();
            requestPermissions();
        } else {
            getLastLocation();
            startLocationUpdates();
        }
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(MappingActivity.this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(MappingActivity.this,
                new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }
    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(MappingActivity.this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("TAG", "Displaying permission rationale to provide additional context.");


        } else {
            Log.i("TAG", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("TAG", "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("TAG", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getLastLocation();
            } else {
                // Permission denied.



            }
        }
    }
    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(MappingActivity.this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            lastLocation = task.getResult();
                            Log.e("TAG", "getLastLocation "+String.valueOf(lastLocation.getLatitude())+","+String.valueOf(lastLocation.getLongitude()));
                        } else {
                            Log.w("ERRO", "getLastLocation:exception", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void showDeleteWarning() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MappingActivity.this);
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View view_ = layoutInflaterAndroid.inflate(R.layout.delete_mapping, null);
        builder.setView(view_);
        builder.setCancelable(false);

        final android.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();
        TextView yesButton = (TextView) view_.findViewById(R.id.done_text_view);
        TextView nobutton = (TextView) view_.findViewById(R.id.skip_text_view);
        TextView txtweight = (TextView) view_.findViewById(R.id.month_text_view);

//        txtweight.setText("Are you Sure You want to Delete/Clear the whole Map.\nDid you consider deleting one point at a time! Confrim yes To delete the whole polygon");

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                for(int i =0; i< arrayMarkers.size(); i++){
                    arrayMarkers.get(i).remove();
                }

                arrayMarkers.clear();
                arrayPoints.clear();
                drawPolyline();
                checkClick = false;
                startedMapping = false;
                doa.deleteExistingCachePolygon();
                Toast.makeText(getApplicationContext(),"DELETED ",Toast.LENGTH_LONG).show();

                if(current_mapping_polygon !=null)
                    current_mapping_polygon.remove();
                alertDialog.dismiss();

            }
        });

        nobutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                alertDialog.dismiss();
            }
        });

    }
    public void open() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MappingActivity.this);
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View view_ = layoutInflaterAndroid.inflate(R.layout.confirm_mapping, null);
        builder.setView(view_);
        builder.setCancelable(false);

        final android.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Button yesButton = (Button)view_.findViewById(R.id.yesButton);
        Button nobutton = (Button)view_.findViewById(R.id.nobutton);
        TextView acerage = (TextView) view_.findViewById(R.id.acerage);
        EditText edt_polygon_name = (EditText) view_.findViewById(R.id.edt_polygon_name);


        area_ = sphutil.computeArea(arrayPoints);
        area_ = doa.round((area_/4047),1);
        acerage.setText(getString(R.string.the_total_land)+" "+(area_));

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (edt_polygon_name.getText().toString().trim().isEmpty()){
                    edt_polygon_name.setError("Required");
                    edt_polygon_name.requestFocus();
                    return;
                }

                int max = 1000000000;
                int min = 100;
                int random = (new Random()).nextInt((max - min) + 1) + min;

                for(int x = 0; x < arrayPoints.size(); x++){

                    LatLng point = arrayPoints.get(x);
                    Double latt = point.latitude;
                    Double longi = point.longitude;

                    ContentValues cv = new ContentValues();
                    cv.put("latitude", latt);
                    cv.put("longi", longi);
                    cv.put("acerage", area_);
                    cv.put("polygon_id", random);
                    cv.put("polygon_name", edt_polygon_name.getText().toString().trim());

                    doa.insertMappedPolygon(cv);
                }

                alertDialog.dismiss();
                arrayPoints.clear();
                mMap.clear();
                isMappingPaused = false;
                startedMapping = false;
                initMappedDataFromDb();
                startmapping.setText("START MAPPING");

            }
        });

        nobutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                alertDialog.dismiss();

            }
        });
    }

    public boolean countPolygonPoints() {
        if (arrayPoints.size() >= 3) {
            checkClick = true;
            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.addAll(arrayPoints);
            polygonOptions.strokeColor(Color.argb(255, 49, 101, 187));
            polygonOptions.strokeWidth(5);
            polygonOptions.fillColor(Color.argb(100, 49, 101, 187));
            current_mapping_polygon = mMap.addPolygon(polygonOptions);

            return true;
        }
        else{
            Toast.makeText(getApplicationContext(), "Too Few points to make a shape file. At least 3 points needed", Toast.LENGTH_LONG).show();

        }
        return false;
    }

    public void startMappingAllowed(){

        startmapping.setText("Mapping in Progress");
        startedMapping = true;

        //check if mapping was stopped for some reason while in progress
//        ArrayList<LatLng> previous_pause_arrayPoints = doa.getProgressMappingReserves();
//        if(previous_pause_arrayPoints !=null && previous_pause_arrayPoints.size() > 5){
//            //Already mapping was in progress and was stopped, so resume from last point
//            for(int i = 0; i < previous_pause_arrayPoints.size(); i++){
//                arrayPoints.add(previous_pause_arrayPoints.get(i));
//                Marker new_marker_point = mMap.addMarker(new MarkerOptions().position(previous_pause_arrayPoints.get(i)).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));
//                arrayMarkers.add(new_marker_point);
//                drawPolyline();
//            }
//
//        }else{
//            arrayPoints=new ArrayList<>();
//        }
//        check if mapping was stopped for some reason while in progress

        arrayPoints=new ArrayList<>();


    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        if (startedMapping){
            if (mapping_mode_type.equals("Manual GPS")){ //picking points

                double distance = getDistanceAtoB(latLng.latitude, latLng.longitude, start_latitude, start_longitude);
                if (distance < 20000000.0) {

                    addPointToCurrentPolyline(latLng);

                } else {

                    Toast.makeText(getApplicationContext(), "Distance too far from Device Location", Toast.LENGTH_LONG).show();

                }
            } else {
                Toast.makeText(getApplicationContext(), "Manual Mapping Not Active", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "You did not select start mapping", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        return false;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.setTrafficEnabled(true);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);


        initMappedDataFromDb();

        /**when a polygon is clicked**/
//        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
//            @Override
//            public void onPolygonClick(@NonNull Polygon polygon) {
//
//            }
//        });
    }

    public void initMappedDataFromDb(){

        LatLng map_center = new LatLng(0.0,0.0);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(map_center, 18)); //19
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));//19
        exisingarrayPoints = existingPolygonPoints(1);
        if(exisingarrayPoints !=null){
            LatLng center_existing = getPolygonCenterPoint(exisingarrayPoints);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center_existing, 14));
        }
        else{
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(map_center, 14));
        }

        //Load other existing polygons
//        new LongOperation().execute("");
    }

    public ArrayList<LatLng> existingPolygonPoints(int polygon_type) {

        /**check for existing polygon**/

        ArrayList<LatLng> exisingarrayPoints = null;
        ArrayList<LatLng> exisingarrayPoints_to_restict = null;



        //loop through polygon id
        if (polygon_type == 1){
            //get polygon ids
            List<String> polygon_ids = doa.fetchExistingPolygonIds();

            Log.e("polygon_ids", "existingPolygonPoints: "+polygon_ids.size());
            for (int x = 0; x < polygon_ids.size(); x++){
                Polygon polygon = null;
                exisingarrayPoints = doa.fetchExistingPolygonLatLngs(polygon_ids.get(x));

                Log.e("exisingarrayPoints", "+++>>: "+exisingarrayPoints.size());

                if (exisingarrayPoints !=null && exisingarrayPoints.size() >= 3){

                    PolygonOptions polygonOptions = new PolygonOptions();
                    polygonOptions.addAll(exisingarrayPoints);
                    polygonOptions.strokeColor(Color.argb(255, 49, 0, 255));
                    polygonOptions.strokeWidth(1);
                    polygonOptions.fillColor(Color.argb(80, 49, 101, 187));
                    polygon = mMap.addPolygon(polygonOptions);

                    //get name assigned to polygon
                    String polygon_name = doa.fetchPolygonAssignedName(polygon_ids.get(x));
                    if (polygon_name != null && !polygon_name.equals("null")) {
                        polygon.setTag(polygon_name);

                        //center camera to polygon center
                        LatLng center_pos = getPolygonCenterPoint(exisingarrayPoints);
                        GroundOverlayOptions iroad = new GroundOverlayOptions();
                        iroad.image(BitmapDescriptorFactory.fromBitmap(textAsBitmap("" +polygon_name, 500, 0xffffffff)));
                        iroad.position(center_pos, 300f);
                        iroad.zIndex(4);
                        iroad.bearing(0);
                        iroad.visible(true);
                        iroad.transparency(0);
                        mMap.addGroundOverlay(iroad);

                        //restring only already mapped polygon
                        exisingarrayPoints_to_restict = exisingarrayPoints;
                    }
                }

                all_polygonBlocksData.add(exisingarrayPoints);

            }
        }

        return exisingarrayPoints_to_restict;
    }

    public Bitmap textAsBitmap(String text, float textSize, int textColor) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent(); // ascent() is negative
        int width = (int) (paint.measureText(text) + 0.5f); // round
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }
    private LatLng getPolygonCenterPoint(ArrayList<LatLng> polygonPointsList){
        LatLng centerLatLng = null;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(int i = 0 ; i < polygonPointsList.size() ; i++)
        {
            builder.include(polygonPointsList.get(i));
        }
        LatLngBounds bounds = builder.build();
        centerLatLng =  bounds.getCenter();

        return centerLatLng;
    }
    private class LongOperation extends AsyncTask<String, Void, WrapperPostExecute> {

        @Override
        protected WrapperPostExecute doInBackground(String... strings) {
            Log.v("START DELAY", "START DELAY");
            sleep(3500);
            //Log.v("END DELAY", "END DELAY");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    existingPolygonPoints(1);
                }
            });

            WrapperPostExecute wO = new WrapperPostExecute();

            return wO;
        }

        @Override
        protected void onPostExecute(WrapperPostExecute wrapperPostExecute) {
            super.onPostExecute(wrapperPostExecute);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }
    public class WrapperPostExecute
    {

    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            Toast.makeText(getApplicationContext(), "Google Play Services is not Available", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public void initLocationServicesNow() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MappingActivity.this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(MappingActivity.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null){

                    /** get current location **/
                    LatLng start_latlng = new LatLng(location.getLatitude(), location.getLongitude());

                    /** set camera to current location **/
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start_latlng, 11));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                }
            }
        });
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(4000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        /** used for receiving notifications when the device location has changed or can no longer be determined **/
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()){

                    LatLng gps_latlng = new LatLng(location.getLatitude(), location.getLongitude());

                    if (startedMapping) {
                        if (location.getAccuracy() <= 30){
                            phoneGpsReceived(gps_latlng); /** checking accuracy of gps location **/
                        } else {
                            Toast.makeText(act, "Bad GPS Accuracy", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        //center one time only
                        one_time_centering = true;
                        start_latitude = gps_latlng.latitude;
                        start_longitude = gps_latlng.longitude;
                    }
                }
            }
        };

    }

    public void phoneGpsReceived(LatLng gps_latlng){

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gps_latlng, 19));  /** camera moves depending with latlng received by phone gps**/

        if (arrayPoints != null && arrayPoints.size() > 0){ //points have been picked

            LatLng previous_saved_point = arrayPoints.get(arrayPoints.size()-1);
            double distance_from_last_point = getDistanceAtoB(previous_saved_point.latitude, previous_saved_point.longitude, gps_latlng.latitude, gps_latlng.longitude);

            if (distance_from_last_point > 5){ /** distance between point received is more than 5 meters**/
                addPointToCurrentPolyline(gps_latlng); //add point received to the polyline
            }
        } else {

            /** this is the first point (NOTE arrayPoints is empty at this point) **/

            addPointToCurrentPolyline(gps_latlng);
        }

        checkClick = true;
    }

    public static float getDistanceAtoB(double latA, double lngA, double latB, double lngB){
        //Returns distance in meters
        Location locationA = new Location("point A");
        locationA.setLatitude(latA);
        locationA.setLongitude(lngA);
        Location locationB = new Location("point B");
        locationB.setLatitude(latB);
        locationB.setLongitude(lngB);

        float distance = locationA.distanceTo(locationB);

        return distance;
    }

    public static void addPointToCurrentPolyline(LatLng latLng){

        boolean is_within = false;

        //check if point is within an existing point
        if (exisingarrayPoints != null && exisingarrayPoints.size() > 0){
            is_within = PolyUtil.containsLocation(latLng, exisingarrayPoints, false);
        }

        is_within = true;
        if (is_within) {

            //checking for overlap here
//            boolean is_point_overlapping_with_other_polygon = false;
//            if(all_polygonBlocksData !=null){ //check if current point is overlapping with other blocks
//                //check if there is a cross with another block - get instersenction between blocks
//                for(int i = 0; i < all_polygonBlocksData.size(); i++){
//                    boolean is_ovelap = isPointOverlapInsideExistingBlockPolygons(latLng, all_polygonBlocksData.get(i));
//                    Log.v("CHECKING If ISNIDE", is_ovelap+" CHECHING POLYGONS OVERLAY "+i);
//                    if(is_ovelap){ //overlap detected
//                        is_point_overlapping_with_other_polygon = true;
//                        break;
//                    }
//                }
//            }
            //checking for overlap here

//            if (!is_point_overlapping_with_other_polygon){
            if (!isMappingPaused){
                Marker new_marker_point = mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));
                arrayPoints.add(latLng);
                arrayMarkers.add(new_marker_point);
                drawPolyline();
                checkClick = true;

                //add to temp table
                ContentValues cv = new ContentValues();
                cv.put("latitude", latLng.latitude);
                cv.put("longi", latLng.longitude);
                cv.put("polygon_id", "only_one");
                doa.insertCachePolygonMappingProgress(cv);

            }
//            } else{ Toast.makeText(cxt, "OVERLAP WITH ANOTHER BLOCK", Toast.LENGTH_LONG).show(); }
        }else{
            Toast.makeText(context, "Point outside Existing Map", Toast.LENGTH_LONG).show();
        }

    }

    public static boolean isPointOverlapInsideExistingBlockPolygons(LatLng current_mapping, ArrayList<LatLng> existing_poly){

        boolean isOnEdge = PolyUtil.isLocationOnEdge(current_mapping,existing_poly,false,6);
        Log.v("========", "isOnEdge "+isOnEdge);

        if(!isOnEdge){
            //check if inside or outside of neighbour blocks
            boolean result = PolyUtil.containsLocation(current_mapping, existing_poly, false);//(new Point(xpoints[i], ypoints[j]));
            if(result) {
                Log.v("========", "xxxxx inside annother found");
                return true;
            }
        }
        return false;
    }

    public static PolylineOptions drawPolyline(){

        PolylineOptions lineOptions = null;
        // Traversing through all the routes
        lineOptions = new PolylineOptions();
        // Adding all the points in the route to LineOptions
        lineOptions.addAll(arrayPoints);
        lineOptions.width(3);
        lineOptions.color(Color.argb(255, 49, 101, 187));

        Log.d("onPostExecute","onPostExecute lineoptions decoded");

        // Drawing polyline in the Google Map for the i-th route
        if(lineOptions != null) {
            if(polylineFinal !=null) {
                polylineFinal.remove();
            }
            polylineFinal = mMap.addPolyline(lineOptions);
        }
        else {
            Log.d("onPostExecute","without Polylines drawn");
        }
        return lineOptions;
    }


    private void stopLocationUpdates() {

        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(MappingActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MappingActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (arrayPoints != null)
            arrayPoints.clear();
        mMap.clear();
        isMappingPaused = false;
        startedMapping = false;
        act.finish();

    }
}