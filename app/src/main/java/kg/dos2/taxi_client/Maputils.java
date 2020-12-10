package kg.dos2.taxi_client;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static kg.dos2.taxi_client.MainActivity.LOG_TAG;
import static kg.dos2.taxi_client.MainFragment.drawableToBitmap;

public class Maputils implements OnMapReadyCallback, MapboxMap.OnCameraIdleListener,
        MapboxMap.OnCameraMoveStartedListener, PermissionsListener {
    private MapboxMap mMap;
    private MapView mv;
    private Marker marker1, marker2, drvMarker;
    private ArrayList<Marker> driversMarker = new ArrayList<>();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Geocoder geocoder;

    private GPSTracker gps;
    private PolylineOptions line;

    private Double lat_1 = 42.876272, lon_1 = 74.606793, lat_2 = 0d, lon_2 = 0d;

    private MyMapListener mapListener;
    private Context context;

    private PermissionsManager permissionsManager;

    public void onResume() {
        if ( mv != null )
            mv.onResume();
    }

    public void onStart() { mv.onStart(); }

    public void onStop() { mv.onStop(); }

    public void onPause() { mv.onPause(); }

    public void onLowMemory() { mv.onLowMemory(); }

    public void onDestroy() {
        if ( mv != null )
            mv.onDestroy();
    }

    public void onSaveInstanceState(Bundle outState) {
        mv.onSaveInstanceState(outState);
    }

    public Maputils(Context context, MyMapListener mapListener, MapView mv, Bundle savedInstanceState) {
        this.mv = mv;
        mv.onCreate(savedInstanceState);
        this.mapListener = mapListener;
        this.context = context;
        try
        {
            mv.getMapAsync(this);
        }
        catch (Exception e)
        {
            Log.e(MainActivity.LOG_TAG, "map init exception " + e.toString());
        }
        geocoder = new Geocoder(context, Locale.getDefault());
    }

    public void removeRouts() {
        try {
            if (mMap.getStyle().getLayer("linelayer") != null)
                mMap.getStyle().removeLayer("linelayer");
            if (mMap.getStyle().getSource("line-source") != null)
                mMap.getStyle().removeSource("line-source");
        } catch (Exception e) {

        }
    }

    public void removeMarkers(int code) {
        switch (code) {
            case 1:
                mMap.removeMarker(marker1);
                break;
            case 2:
                mMap.removeMarker(marker2);
                break;
        }
    }

    public void addDriverMarker(double lat, double lng) {
        Log.e(LOG_TAG, "MapUtils addDriverMarker");
        // Create an Icon object for the marker to use
        IconFactory iconFactory = IconFactory.getInstance(context);
        Icon icon_drv = iconFactory.fromBitmap(drawableToBitmap(context.getResources().getDrawable(R.drawable.taxi_marker)));
        LatLng coordinate = new LatLng(lat, lng);
        MarkerOptions markerOptions = new MarkerOptions().position(coordinate)
                .title("Водитель").setIcon(icon_drv);
        if ( drvMarker != null ) mMap.removeMarker(drvMarker);
        drvMarker = mMap.addMarker(markerOptions);

        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 16);
        mMap.animateCamera(yourLocation);
    }

    public void setDriverMarkerPosition(double lat, double lng) {
        Log.e(LOG_TAG, "MapUtils setDriverMarkerPosition");
        if ( drvMarker != null )
            drvMarker.setPosition(new LatLng(lat, lng));
        else addDriverMarker(lat, lng);
    }

    public void toMyLocation(double lat, double lng) {
        LatLng coordinate = new LatLng(lat, lng);
        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 16);
        mMap.animateCamera(yourLocation);
    }

    public void zoomOut() {
        LatLng coord = new LatLng(mMap.getCameraPosition().target);
        CameraUpdate loc = CameraUpdateFactory.newLatLngZoom(coord,
                mMap.getCameraPosition().zoom - 0.1);
        mMap.animateCamera(loc);
    }

    public void zoomIn() {
        LatLng coord1 = new LatLng(mMap.getCameraPosition().target);
        CameraUpdate loc1 = CameraUpdateFactory.newLatLngZoom(coord1,
                mMap.getCameraPosition().zoom + 0.1);
        mMap.animateCamera(loc1);
    }

    public void pmrk() {
        lat_1 = Prefs.currOrder.getDouble("lat1");
        lon_1 = Prefs.currOrder.getDouble("lon1");
        lat_2 = Prefs.currOrder.getDouble("lat2");
        lon_2 = Prefs.currOrder.getDouble("lon2");

        // Create an Icon object for the marker to use
        IconFactory iconFactory = IconFactory.getInstance(context);
        Icon icon_a = iconFactory.fromBitmap(drawableToBitmap(context.getResources().getDrawable(R.drawable.ic_point_a)));
        Icon icon_b = iconFactory.fromBitmap(drawableToBitmap(context.getResources().getDrawable(R.drawable.ic_point_b)));
        if (marker1 != null) {
            marker1.setPosition(new LatLng(lat_1, lon_1));
        } else {
            MarkerOptions mo1 = new MarkerOptions().position(new LatLng(lat_1, lon_1))
                    .title("Откуда").setIcon(icon_a);
            marker1 = mMap.addMarker(mo1);

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(new LatLng(lat_1, lon_1), 16);
            mMap.animateCamera(yourLocation);
        }

        if (marker2 != null) {
            marker2.setPosition(new LatLng(lat_2, lon_2));
        } else {
            MarkerOptions mo2 = new MarkerOptions().position(new LatLng(lat_2, lon_2))
                    .title("Куда").setIcon(icon_b);
            marker2 = mMap.addMarker(mo2);
        }
    }

    private void drawLines(@NonNull FeatureCollection featureCollection) {
        if (mMap != null) {
            mMap.getStyle(style -> {
                if (featureCollection.features() != null) {
                    if (featureCollection.features().size() > 0) {
                        style.removeLayer("linelayer");
                        style.removeSource("line-source");
                        style.addSource(new GeoJsonSource("line-source", featureCollection));
                        // The layer properties for our line. This is where we make the line dotted, set the
                        // color, etc.
                        style.addLayer(new LineLayer("linelayer", "line-source")
                                .withProperties(PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
                                        PropertyFactory.lineJoin(Property.LINE_JOIN_MITER),
                                        PropertyFactory.lineOpacity(.7f),
                                        PropertyFactory.lineWidth(7f),
                                        PropertyFactory.lineColor(Color.parseColor("#3bb2d0"))));
                    }
                }
            });
        }
    }

    void loadGeoJson() {
        loadGeoJson(lon_1, lat_1, lon_2, lat_2);
    }

    public void loadGeoJson(double lat1, double lon1, double lat2, double lon2) {
        String url = "https://api.mapbox.com/directions/v5/mapbox/driving/"+lat1+","+lon1+";"+lat2+","+lon2+
                "?geometries=geojson&access_token=" +
                "pk.eyJ1IjoidGFhbGF5ZGV2IiwiYSI6ImNrM3JyemJqbzBkMnAzaHBzaTNtbWE0eDYifQ.6jS_pnpk66YaDc2EabZF4A";

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
                    JSONObject obj = new JSONObject(s);
                    JSONArray arr = obj.getJSONArray("routes");
                    String tmp = "{" +
                            "\"type\": \"FeatureCollection\"," +
                            "\"features\": [{" +
                            "\"type\": \"Feature\"," +
                            "\"properties\": {" +
                            "\"name\": \"\"" +
                            "},\"geometry\": " +
                                arr.getJSONObject(0).getJSONObject("geometry") +
                            "}]" +
                            "}";
                    drawLines(FeatureCollection.fromJson(tmp));
                    mapListener.getDistance(arr.getJSONObject(0).getDouble("distance"));
                    mapListener.afterLoadGeoJSON();
                    Log.e(LOG_TAG, "loadGeoJson distance " + arr.getJSONObject(0).getInt("distance"));
                } catch (JSONException je) {
                    Log.e("myimag", "json exception " + je);
                } catch (Exception e) {
                    Log.e("myimag", "catException " + e);
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("myimag", "VolleyError: " + volleyError);

            }
        });
        RequestQueue rQueue = Volley.newRequestQueue(context);
        rQueue.add(request);
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(context)) {

            // Get an instance of the component
            LocationComponent locationComponent = mMap.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, loadedMapStyle).build());

            // Enable to make component visible
            // locationComponent.setLocationComponentEnabled(true);

            // Set the component's render mode
            // locationComponent.setRenderMode(RenderMode.GPS);
            // locationComponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(mapListener.getAct());
        }
    }

    void clearDriversMarker() {
        for ( int i = 0; i < driversMarker.size(); i++ ) {
            mMap.removeMarker(driversMarker.get(i));
        }
        driversMarker.clear();
    }

    void addDriversMarker(double lat, double lng, int id) {
        // Create an Icon object for the marker to use
        IconFactory iconFactory = IconFactory.getInstance(context);
        Icon icon_drv = iconFactory.fromBitmap(drawableToBitmap(context.getResources().getDrawable(R.drawable.taxi_marker)));
        MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(lat, lng))
                .title("#" + id);
        Marker mrk = mMap.addMarker(markerOptions);
        driversMarker.add(mrk);
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        // Create an Icon object for the marker to use
        IconFactory iconFactory = IconFactory.getInstance(context);
        Icon icon_a = iconFactory.fromBitmap(drawableToBitmap(context.getResources().getDrawable(R.drawable.ic_point_a)));
        try {

            mMap = mapboxMap;
            // mMap.getUiSettings().setZoomControlsEnabled(true);
            mapboxMap.setStyle(Style.MAPBOX_STREETS,
                    new Style.OnStyleLoaded() {
                        @Override
                        public void onStyleLoaded(@NonNull Style style) {
                            enableLocationComponent(style);
                            try {
                                Layer mapText = style.getLayer("country-label");
                                mapText.setProperties(textField("{name_ru}"));
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "country set exception " + e.toString());
                            }
                        }
                    });
            mMap.getUiSettings().setZoomGesturesEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(true);
            // mMap.getUiSettings().setMyLocationButtonEnabled(true);
            // mMap.setMyLocationEnabled(true);
            // mMap.getLocationComponent().setLocationComponentEnabled(true);

            mMap.addOnCameraIdleListener(this);
            mMap.addOnCameraMoveStartedListener(this);

            mMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                @Override
                public boolean onMapClick(@NonNull LatLng point) {
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        try {
            if ( gps == null )
                gps = new GPSTracker(context);
            if (gps.canGetLocation()) {
                lat_1 = gps.getLatitude();
                lon_1 = gps.getLongitude();
            } else {
                //       gps.showSettingsAlert();
            }
            LatLng coordinate = new LatLng(lat_1, lon_1);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 16);
            mMap.animateCamera(yourLocation);

            MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(lat_1, lon_1))
                    .title("Откуда").setIcon(icon_a);
            marker1 = mMap.addMarker(markerOptions);

            GetAddressLine(lat_1, lon_1);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        // check if map is created successfully or not
        if (mMap == null)
        {
            Toast.makeText(context, "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
        }
    }

    public void GetAddressLine(double lat, double lon) {
        Geocoder myLocation = new Geocoder(context,
                Locale.getDefault());
        try {
            List<Address> myList = myLocation.getFromLocation(lat, lon, 1);

            if (myList != null && myList.size() > 0) {
                Address address = myList.get(0);
                String street = address.getThoroughfare();
                mapListener.getStreet(street);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraIdle() {
        // Create an Icon object for the marker to use
        IconFactory iconFactory = IconFactory.getInstance(context);
        Icon icon_a = iconFactory.fromBitmap(drawableToBitmap(context.getResources().getDrawable(R.drawable.ic_point_a)));
        Icon icon_b = iconFactory.fromBitmap(drawableToBitmap(context.getResources().getDrawable(R.drawable.ic_point_b)));

        LatLng point = mMap.getCameraPosition().target;
        if ( mapListener.getStep() == 0 ) {
            try {
                lat_1 = point.getLatitude();
                lon_1 = point.getLongitude();
                marker1.setPosition(point);

                GetAddressLine(point.getLatitude(), point.getLongitude());
            } catch (Exception exc) {
                Log.e(LOG_TAG, exc.toString());
            }
        } else if ( mapListener.getStep() == 1 ) {
            try {
                if (marker2 == null) {

                    lat_2 = point.getLatitude();
                    lon_2 = point.getLongitude();
                    MarkerOptions markerOptions = new MarkerOptions().position(point)
                            .title("Куда").setIcon(icon_b);
                    marker2 = mMap.addMarker(markerOptions);

                    GetAddressLine(point.getLatitude(), point.getLongitude());
                } else {
                    lat_2 = point.getLatitude();
                    lon_2 = point.getLongitude();
                    marker2.setPosition(point);

                    GetAddressLine(point.getLatitude(), point.getLongitude());
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "OnCameraIdleExc " + e.toString());
            }
        }
    }

    @Override
    public void onCameraMoveStarted(int reason) {

    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {

    }

    public Double getLat1() {
        return lat_1;
    }

    public Double getLat2() {
        return lat_2;
    }

    public Double getLon1() {
        return lon_1;
    }

    public Double getLon2() {
        return lon_2;
    }
}
