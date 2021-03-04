package com.hhk.customusemap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    static final int PERMISSION_REQUEST_CODE = 0x1000;
    static final String TAG = "TAGNOW";
    static final String RESTAPIKEY = "5257c07eaaf1b47c1178f208ba98a71d";
    static final String ACTIVITY_TRANSFER_KEY_ADDRNAME = "ACTIVITY_TRANSFER_KEY_ADDRNAME";
    static final String GEO_COORD_LAT_TRANSFER_KEY = "GEO_COORD_LAT_TRANSFER_KEY";
    static final String GEO_COORD_LON_TRANSFER_KEY = "GEO_COORD_LON_TRANSFER_KEY";
    static final String INTENT_DATA_ITEM_ID = "INTENT_DATA_ITEM_ID";
    static final String INTENT_DATA_PHONENUMBER = "INTENT_DATA_PHONENUMBER";
    static final String INTENT_DATA_IDENTIFYNAME = "INTENT_DATA_IDENTIFYNAME";
    static final int MARKER_CURRENT_LOCATION_POI_TAG_VALUE = -1;
    static final int ACTIVITY_REQUESTCODE_LocationSearchingActivity = 0x100;
    static final int ACTIVITY_REQUESTCODE_LocationMiscInfoInputActivity = 0x200;
    static final int ACTIVITY_REQUESTCODE_CardListActivity = 0x300;
    static final int ACTIVITY_REQUESTCODE_MISCINFOSHOWPANEL = 0x400;
    static final int ACTIVITY_REQUESTCODE_USER_LOGIN = 0x500;
    final String SHAREDPREFERANCE_DATA_LOGIN_ID_KEYSTRING = "LOGIN_ID";

    private boolean ispositionselecting = false;
    double poitouched_lat = 0.00;
    double poitouched_lon = 0.00;

    Animation fab_open, fab_close;
    Boolean isFabOpen = false;
    FloatingActionButton flb_currentposition, flb_positionningselect, flb_poi_off_all;
    MapView mapView;
    ImageView pin_centered;
    ArrayList<marker_selectable_item> marker_selectable_itemArrayList;
    SharedPreferences sharedPreferences;

    String ThisPhoneLineNumber = null;
    String UserIdentifyName = null;

    SimpleLoadingProgressbarDialog slpdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout();
        reqpermission();
        marker_selectable_itemArrayList = new ArrayList<marker_selectable_item>();

        Log.d(TAG, "ThisPhoneLineNumber : " + ThisPhoneLineNumber);

        mapView = new MapView(this);
        mapView.setMapViewEventListener(mapViewEventListener);
        mapView.setPOIItemEventListener(poiItemEventListener);
        ViewGroup mapViewContainer = findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

        long size = 0;
        for( File file : getCacheDir().listFiles()) size += file.length();
        Log.d(TAG, "cachedir totalsize : " + size);
//        getHashKey();

        slpdialog = new SimpleLoadingProgressbarDialog(MainActivity.this);
        slpdialog.show();
    }

    private void getHashKey() {
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null)
            Log.e(TAG, "KeyHash:null");
        assert packageInfo != null;
        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d(TAG, "KEYHASH : " + Base64.encodeToString(md.digest(), Base64.DEFAULT));
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "Unable to get MessageDigest. signature=" + signature, e);
            }
        }
    }

    void checkUSERIDNameAndSetup() {
        sharedPreferences = getSharedPreferences("LoginProfile", MODE_PRIVATE);
        UserIdentifyName = sharedPreferences.getString(SHAREDPREFERANCE_DATA_LOGIN_ID_KEYSTRING, "null");
        assert UserIdentifyName != null;
        //TODO temporary disabled
        if (UserIdentifyName.equals("null")) {
            Intent intent = new Intent(MainActivity.this.getApplicationContext(), UserLoginActivity.class);
            intent.putExtra(INTENT_DATA_PHONENUMBER, ThisPhoneLineNumber);
            startActivityForResult(intent, ACTIVITY_REQUESTCODE_USER_LOGIN);
        } else {
            Log.d(TAG, "UserIdentifyName is " + UserIdentifyName);
        }
    }

    @SuppressLint("HardwareIds")
    void reqpermission() {
        String[] permissionsrequired = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.READ_PHONE_STATE};
        if (ContextCompat.checkSelfPermission(MainActivity.this, permissionsrequired[0]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permissionsrequired[1]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permissionsrequired[2]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permissionsrequired[3]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permissionsrequired[4]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, permissionsrequired[5]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, permissionsrequired, PERMISSION_REQUEST_CODE);
            return;
        }

        //TODO Erase This line
        ThisPhoneLineNumber="Ttettesetsetset";

        // TODO temporary diabled
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try {
            if (telephonyManager.getLine1Number() != null) {
                ThisPhoneLineNumber = telephonyManager.getLine1Number();
            } else {
                if (telephonyManager.getSimSerialNumber() != null) {
                    ThisPhoneLineNumber = telephonyManager.getSimSerialNumber();
                }
            }
            if(ThisPhoneLineNumber != null) {
                if (ThisPhoneLineNumber.startsWith("+82")) {
                    ThisPhoneLineNumber = ThisPhoneLineNumber.replace("+82", "0");
                }
            }
            else {
                Toast.makeText(this, getString(R.string.PhonenumberRetrieveError), Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.PhonenumberRetrieveError), Toast.LENGTH_SHORT).show();
            finish();
        }

        checkUSERIDNameAndSetup();
        Log.d(TAG, "rightly phonenumber retrieved : " + ThisPhoneLineNumber);
    }

    void layout() {
        pin_centered = findViewById(R.id.pin_centered);
        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);

        flb_poi_off_all = findViewById(R.id.flb_poi_off_all);
        flb_currentposition = findViewById(R.id.flb_currentposition);
        flb_positionningselect = findViewById(R.id.flb_positionningselect);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.btnmenu_search) {
            Intent intent = new Intent(getApplicationContext(), LocationSearchingActivity.class);
            startActivityForResult(intent, ACTIVITY_REQUESTCODE_LocationSearchingActivity);
            return true;
        } else if (item.getItemId() == R.id.btnmenu_list) {
            Intent intent = new Intent(getApplicationContext(), CardListActivity.class);
            intent.putExtra(INTENT_DATA_IDENTIFYNAME, UserIdentifyName);
            startActivityForResult(intent, ACTIVITY_REQUESTCODE_CardListActivity);
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case ACTIVITY_REQUESTCODE_LocationSearchingActivity: {
                if (resultCode == Activity.RESULT_OK) {
                    assert data != null;
//                String MapAddressName = data.getStringExtra(LocationSearchingActivity.INTENT_DATA_MAPADDRNAME);
//                String MapAddreesType = data.getStringExtra(LocationSearchingActivity.INTENT_DATA_MAPADDRTYPE);
                    double MapAddressLatitude = data.getDoubleExtra(LocationSearchingActivity.INTENT_DATA_LATITUDE, -1);
                    double MapAddressLongitude = data.getDoubleExtra(LocationSearchingActivity.INTENT_DATA_LONGITUDE, -1);

                    Log.d(MainActivity.TAG, "lat:" + MapAddressLatitude + ",lon:" + MapAddressLongitude);

                    MapPoint mapPoint;
                    mapPoint = MapPoint.mapPointWithGeoCoord(MapAddressLatitude, MapAddressLongitude);

                    current_location_marker_set(mapPoint, getString(R.string.currentlocation), true);

                    mapView.setMapCenterPointAndZoomLevel(mapPoint, 2, true);
                }
            } break;
            case ACTIVITY_REQUESTCODE_CardListActivity: {
                if (resultCode == Activity.RESULT_OK) {
                    int id;
                    String title;
                    double lat, lon;

                    assert data != null;
                    id = Integer.parseInt(data.getStringExtra(CardListActivity.INTENT_DATA_FROM_CARDLIST_ID));
                    title = data.getStringExtra(CardListActivity.INTENT_DATA_FROM_CARDLIST_TITLE);
                    lat = data.getDoubleExtra(CardListActivity.INTENT_DATA_FROM_CARDLIST_LAT, -1);
                    lon = data.getDoubleExtra(CardListActivity.INTENT_DATA_FROM_CARDLIST_LON, -1);

                    if (lat == -1 || lon == -1) {
                        Toast.makeText(this, "error while activity data transfer", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Marker_Items_Add_and_positioning(id, title, lat, lon, true);
                }
            } break;
            case ACTIVITY_REQUESTCODE_LocationMiscInfoInputActivity: {
                if (resultCode == Activity.RESULT_OK) {
                    int id;
                    String title;
                    double lat, lon;

                    assert data != null;
                    id = data.getIntExtra(LocationMiscInputActivity.INTENT_DATA_FROM_INPUTACTIVITY_ID, -1);
                    title = data.getStringExtra(LocationMiscInputActivity.INTENT_DATA_FROM_INPUTACTIVITY_TITLE);
                    lat = data.getDoubleExtra(LocationMiscInputActivity.INTENT_DATA_FROM_INPUTACTIVITY_LAT, -1);
                    lon = data.getDoubleExtra(LocationMiscInputActivity.INTENT_DATA_FROM_INPUTACTIVITY_LON, -1);

                    if (id == -1 || lat == -1 || lon == -1) {
                        Toast.makeText(this, "error while activity data transfer", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (MapPOIItem item : mapView.getPOIItems()) {
                        if (item.getTag() == MARKER_CURRENT_LOCATION_POI_TAG_VALUE) {
                            mapView.removePOIItem(item);
                        }
                    }

                    Marker_Items_Add_and_positioning(id, title, lat, lon, true);
                }
            } break;
            case ACTIVITY_REQUESTCODE_USER_LOGIN: {
                if (resultCode == Activity.RESULT_OK) {
                    assert data != null;
                    UserIdentifyName = data.getStringExtra(UserLoginActivity.INTENT_DATA_FROM_USER_LOGIN);
                    assert UserIdentifyName != null;
                    Log.d(TAG, "ID is SET : " + UserIdentifyName);
                    SharedPreferences.Editor editor;
                    editor = sharedPreferences.edit();
                    editor.putString(SHAREDPREFERANCE_DATA_LOGIN_ID_KEYSTRING, UserIdentifyName);
                    editor.apply();
                } else {
                    Toast.makeText(this, getString(R.string.ThatwasEssantialInput), Toast.LENGTH_SHORT).show();
                    finish();
                }
            } break;
            case ACTIVITY_REQUESTCODE_MISCINFOSHOWPANEL: {
                if(resultCode == Activity.RESULT_OK) {
                    assert data != null;
                    int poiitemtag = data.getIntExtra(DetailedInfoPanel.INTENT_DATA_FROM_POI_DELETE_REQUEST, -1);
                    for( MapPOIItem item : mapView.getPOIItems()) {
                        if(item.getTag() == poiitemtag) {
                            mapView.removePOIItem(item);
                            break;
                        }
                    }
                }
            }
        }
    }

    void Marker_Items_Add_and_positioning(int id, String title, double items_lat, double items_lon, boolean positioning) {
        double addmarker_lat = -1;
        double addmarker_lon = -1;

        for (MapPOIItem item : mapView.getPOIItems()) {
            if (item.getTag() == id) {
                mapView.removePOIItem(item);
            }
            if(item.getTag() == MARKER_CURRENT_LOCATION_POI_TAG_VALUE) {
                MapPoint.GeoCoordinate coord = item.getMapPoint().getMapPointGeoCoord();
                addmarker_lat = coord.latitude;
                addmarker_lon = coord.longitude;
            }
        }

        Log.d(TAG, "addmarker : " + addmarker_lat + ", " + addmarker_lon);
        Log.d(TAG, "selectmarker : " +  items_lat + ", " + items_lon);
        //TODO compare above two location params

        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(items_lat, items_lon);
//        location_info_item_poi_add(id, title, mapPoint);

        MapPOIItem marker = new MapPOIItem();
        marker.setTag(id);
        marker.setItemName(title);
        marker.setMapPoint(mapPoint);
        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
        marker.setCustomImageResourceId(R.drawable.baseline_room_black_24dp);
        marker.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage);
        marker.setCustomSelectedImageResourceId(R.drawable.baseline_room_red_24dp);

        marker.setCustomImageAutoscale(true);
        marker.setCustomImageAnchor(0.5f, 1.0f);

        mapView.addPOIItem(marker);

        if(positioning)
            mapView.setMapCenterPointAndZoomLevel(mapPoint, 2, true);

        marker_selectable_itemArrayList.add(new marker_selectable_item(id));
    }

    @SuppressLint("HardwareIds")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    Toast.makeText(this, "permission not granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            if (granted) {

                //TODO Erase This line
                ThisPhoneLineNumber="Ttettesetsetset";

                // TODO temporary diabled
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) !=
                            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) !=
                            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) !=
                            PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if (telephonyManager.getLine1Number() != null) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) !=
                                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) !=
                                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) !=
                                PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        ThisPhoneLineNumber = telephonyManager.getLine1Number();
                    } else {
                        if (telephonyManager.getSimSerialNumber() != null) {
                            ThisPhoneLineNumber = telephonyManager.getSimSerialNumber();
                        }
                    }
                    if(ThisPhoneLineNumber != null) {
                        if (ThisPhoneLineNumber.startsWith("+82")) {
                            ThisPhoneLineNumber = ThisPhoneLineNumber.replace("+82", "0");
                        }
                    }
                    else {
                        Toast.makeText(this, getString(R.string.PhonenumberRetrieveError), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, getString(R.string.PhonenumberRetrieveError), Toast.LENGTH_SHORT).show();
                    finish();
                }

                Log.d(TAG, "after grant retrieved : " + ThisPhoneLineNumber);
                CenterSetByGps(true);
                checkUSERIDNameAndSetup();
            }
        }
    }

    boolean GpsRetrieveProcess(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(latitude,longitude, 7);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
            return false;
        }

        if(addresses == null || addresses.size() == 0) {
            Log.d(TAG, "address retrieve failed");
            return false;
        }

        Address address = addresses.get(0);
        Log.d(TAG, address.getAddressLine(0));
        return true;
    }

    void CenterSetByGps(boolean addaddmarker) {
        double lat, lon;
        GpsTracker gpsTracker = new GpsTracker(MainActivity.this);
        lat = gpsTracker.getLatitude();
        lon = gpsTracker.getLongitude();
        MapPoint mapPoint;
        if(GpsRetrieveProcess(lat, lon)) {
            mapPoint = MapPoint.mapPointWithGeoCoord(lat,lon);
//                        Toast.makeText(MainActivity.this, lat + ", " + lon, Toast.LENGTH_SHORT).show();
            Log.d(TAG, lat + ", " + lon);
        }
        else {
            Toast.makeText(MainActivity.this, "failed gps info", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "failed gps info");
            return;
        }

        if(addaddmarker)
            current_location_marker_set(mapPoint, getString(R.string.currentlocation), true);
        else
            current_location_marker_set(mapPoint, getString(R.string.currentlocation), false);
        mapView.setMapCenterPointAndZoomLevel(mapPoint, 2, true);
    }

    public void anim() {
        if (isFabOpen) {
            flb_currentposition.startAnimation(fab_close);
            flb_positionningselect.startAnimation(fab_close);
            flb_poi_off_all.startAnimation(fab_close);
            flb_currentposition.setClickable(false);
            flb_positionningselect.setClickable(false);
            flb_poi_off_all.setClickable(false);
            isFabOpen = false;
        } else {
            flb_currentposition.startAnimation(fab_open);
            flb_positionningselect.startAnimation(fab_open);
            flb_poi_off_all.startAnimation(fab_open);
            flb_currentposition.setClickable(true);
            flb_positionningselect.setClickable(true);
            flb_poi_off_all.setClickable(true);
            isFabOpen = true;
        }
    }

    public void onfabClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.fab1:
                anim();
                positionning_end();
                ispositionselecting = false;
                flb_positionningselect.setImageResource(R.drawable.ic_add_location_alt_white_24dp);
                break;
            case R.id.flb_currentposition: {
                CenterSetByGps(false);
                positionning_end();
                ispositionselecting = false;
                flb_positionningselect.setImageResource(R.drawable.ic_add_location_alt_white_24dp);
                } break;
            case R.id.flb_positionningselect: {
                ispositionselecting = !ispositionselecting;
                if(ispositionselecting) {
                    positionning_started();
                }
                else {
                    positionning_end();
                    MapPoint mapPoint = mapView.getMapCenterPoint();
                    current_location_marker_set(mapPoint, getString(R.string.SelectedLocationText), true);
                }
            } break;
            case R.id.flb_poi_off_all: {

                for(MapPOIItem item : mapView.getPOIItems())
//                    if(item.getTag() != MARKER_CURRENT_LOCATION_POI_TAG_VALUE)
                        mapView.removePOIItem(item);

                marker_selectable_itemArrayList = new ArrayList<marker_selectable_item>();
            } break;
        }
    }

    private void positionning_started() {

        for(MapPOIItem item : mapView.getPOIItems()) {
            if (item.getTag() == MARKER_CURRENT_LOCATION_POI_TAG_VALUE) {
                mapView.removePOIItem(item);
            }
        }

        flb_positionningselect.setImageResource(R.drawable.ic_done_white_24dp);
        pin_centered.setVisibility(View.VISIBLE);
    }

    private void positionning_end() {
        flb_positionningselect.setImageResource(R.drawable.ic_add_location_alt_white_24dp);
        pin_centered.setVisibility(View.INVISIBLE);
    }


    private void current_location_marker_set(MapPoint mapPoint, String poistr , boolean is_addmarker) {
        for(MapPOIItem item : mapView.getPOIItems()) {
            if( item.getTag() == MARKER_CURRENT_LOCATION_POI_TAG_VALUE) {
                mapView.removePOIItem(item);

                MapPOIItem marker = new MapPOIItem();
                marker.setTag(MARKER_CURRENT_LOCATION_POI_TAG_VALUE);
                marker.setItemName(poistr);
                marker.setMapPoint(mapPoint);
                marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);

                if(is_addmarker)
                    marker.setCustomImageResourceId(R.drawable.baseline_add_location_black_48dp);
//                else marker.setCustomImageResourceId(R.drawable.baseline_my_location_black_36dp);

                marker.setCustomImageAutoscale(true);
                marker.setCustomImageAnchor(0.5f, 1.0f);
                mapView.addPOIItem(marker);

                return;
            }
        }

        MapPOIItem marker = new MapPOIItem();
        marker.setTag(MARKER_CURRENT_LOCATION_POI_TAG_VALUE);
        marker.setItemName(poistr);
        marker.setMapPoint(mapPoint);
        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);

        if(is_addmarker)
            marker.setCustomImageResourceId(R.drawable.baseline_add_location_black_48dp);
//        else marker.setCustomImageResourceId(R.drawable.baseline_my_location_black_36dp);

        marker.setCustomImageAutoscale(true);
        marker.setCustomImageAnchor(0.5f, 1.0f);
        mapView.addPOIItem(marker);
    }


    MapView.MapViewEventListener mapViewEventListener = new MapView.MapViewEventListener() {
        @Override
        public void onMapViewInitialized(MapView mapView) {
            CenterSetByGps(false);
            initial_spread_marker_thread = new initial_spread_marker();
            initial_spread_marker_thread.start();
        }

        @Override
        public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {
        }
        @Override
        public void onMapViewZoomLevelChanged(MapView mapView, int i) {
        }
        @Override
        public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {
        }
        @Override
        public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {
        }
        @Override
        public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {
        }
        @Override
        public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {
        }
        @Override
        public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {
        }
        @Override
        public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {
        }
    };

    MapView.POIItemEventListener poiItemEventListener = new MapView.POIItemEventListener() {
        @Override
        public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
        }

        @Override
        public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
            if( mapPOIItem.getTag() == MARKER_CURRENT_LOCATION_POI_TAG_VALUE) {
                MapPoint mapPoint = mapPOIItem.getMapPoint();
                poitouched_lat = mapPoint.getMapPointGeoCoord().latitude;
                poitouched_lon = mapPoint.getMapPointGeoCoord().longitude;

                MapReverseGeoCoder mapReverseGeoCoder = new MapReverseGeoCoder(RESTAPIKEY, mapPoint, reverseGeoCodingResultListener,MainActivity.this);
                mapReverseGeoCoder.startFindingAddress();
            }
            else {
                for(marker_selectable_item item : marker_selectable_itemArrayList) {
                    if(mapPOIItem.getTag() == item.getId()) {
                        Intent intent = new Intent(MainActivity.this.getApplicationContext(), DetailedInfoPanel.class);
                        intent.putExtra(INTENT_DATA_ITEM_ID, item.getId());
                        intent.putExtra(INTENT_DATA_IDENTIFYNAME, UserIdentifyName);
                        startActivityForResult(intent, ACTIVITY_REQUESTCODE_MISCINFOSHOWPANEL);
                        break;
                    }
                }
            }
        }

        @Override
        public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
        }

        @Override
        public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {
        }
    };

    MapReverseGeoCoder.ReverseGeoCodingResultListener reverseGeoCodingResultListener = new MapReverseGeoCoder.ReverseGeoCodingResultListener() {
        @Override
        public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
            Intent intent = new Intent(MainActivity.this.getApplicationContext(), LocationMiscInputActivity.class);
            intent.putExtra(ACTIVITY_TRANSFER_KEY_ADDRNAME, s);
            intent.putExtra(GEO_COORD_LAT_TRANSFER_KEY, poitouched_lat);
            intent.putExtra(GEO_COORD_LON_TRANSFER_KEY, poitouched_lon);
            intent.putExtra(INTENT_DATA_PHONENUMBER, ThisPhoneLineNumber);
            startActivityForResult(intent,ACTIVITY_REQUESTCODE_LocationMiscInfoInputActivity);
        }

        @Override
        public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
            Toast.makeText(MainActivity.this, getString(R.string.ReverseGeoCoderFailedText), Toast.LENGTH_SHORT).show();
        }
    };





    initial_spread_marker initial_spread_marker_thread;
    private class initial_spread_marker extends Thread {

        private String requestdata() throws Exception {
            HttpURLConnection connection;
            URL url = new URL(getString(R.string.Server_Inet_ADDR)+"?method=1");
            connection = (HttpURLConnection)url.openConnection();

            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String str;
            StringBuilder buffer = new StringBuilder();
            while((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            connection.disconnect();
            return buffer.toString();
        }
        @Override
        public void run() {
            boolean success = false;
            try {
                String jsonraw = requestdata();
                JSONObject jsonObjectSrc;
                jsonObjectSrc = new JSONObject(jsonraw);
                String nowpagecount = jsonObjectSrc.getString("work");
                if(nowpagecount.equals("select")) {
                    JSONArray jsonArray = jsonObjectSrc.getJSONArray("result");
                    for(int i=0;i<jsonArray.length();i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        int id ;String title;double lat, lon;
                        id = object.getInt("id");
                        title = object.getString("title");
                        lat = object.getDouble("lat");
                        lon = object.getDouble("lon");
                        Marker_Items_Add_and_positioning(id, title,lat,lon, false);
                    }
                    success = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(!success) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "marker spread failed - somthing wrong", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            slpdialog.dismiss();
        }
    }

}
