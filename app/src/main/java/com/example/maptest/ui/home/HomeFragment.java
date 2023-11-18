package com.example.maptest.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.maptest.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    SupportMapFragment mapFragment;
    Context mThis;
    Activity mActivity;
    private GoogleMap googleMap;
    private MapView mapView;
    private Marker currentLocationMarker;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private AlertDialog locationSettingsDialog;
    private Geocoder geocoder;
    private boolean isFirstLocationUpdate = true;
    EditText sourceEditText;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mThis = getContext();
        mActivity = getActivity();
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        sourceEditText = rootView.findViewById(R.id.sourceEditText);
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        // Check and request location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestLocationPermissions();
        } else {
            // Permissions are already granted for devices below Android 6.0
            initMap();
//            getLocation();
        }

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return rootView;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(mThis, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, get the location
//            getLocation();
            initMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get the location
//                getLocation();
                initMap();
            } else {
                // Permission denied, handle accordingly
            }
        }
    }
    private void initMap() {
        if (mapView != null) {
            mapView.onCreate(null);
            mapView.onResume();
            mapView.getMapAsync(this);
        }
    }

    // Add the method to get the location

    private void getLocation() {
        LocationManager locationManager = (LocationManager) mThis.getSystemService(Context.LOCATION_SERVICE);

        // Check if location services are enabled
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Request location updates
            if (ActivityCompat.checkSelfPermission(mThis, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mThis, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0, // minimum time interval between updates in milliseconds
                    0, // minimum distance between updates in meters
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            // Handle the location change
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Toast.makeText(mActivity, "Location: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                            showLocationSettingsDialog();
                        }
                    }
            );
        } else {
            // Location services are not enabled, prompt the user to enable them
            // You may want to redirect the user to the location settings page
            showLocationSettingsDialog();
            Toast.makeText(mThis, "Please enable location services", Toast.LENGTH_SHORT).show();
        }
    }
    private void showLocationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Location Services Disabled")
                .setMessage("Please enable location services to use this feature.")
                .setPositiveButton("Settings", (dialog, which) -> openLocationSettings())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        locationSettingsDialog = builder.create();

        // Show the dialog
        locationSettingsDialog.show();
    }

    private void openLocationSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }
    private void dismissLocationSettingsDialog() {
        // Dismiss the location settings dialog if it is currently showing
        if (locationSettingsDialog != null && locationSettingsDialog.isShowing()) {
            locationSettingsDialog.dismiss();
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Enable location layer (blue dot)
        if (ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        // Initialize the geocoder
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        // Set up location updates
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (isFirstLocationUpdate) {
                        // Handle the location change
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        LatLng latLng = new LatLng(latitude, longitude);

                        // Remove the previous marker if exists
                        if (currentLocationMarker != null) {
                            currentLocationMarker.remove();
                        }

                        // Add a marker at the current location
                        currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title("Current Location"));

                        // Move the camera to the current location
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        // Display the name of the current location in a toast
                        showLocationName(latitude, longitude);
                        // Remove location updates after the first successful update
                        locationManager.removeUpdates(this);
                        isFirstLocationUpdate = false;
                    }

                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                    dismissLocationSettingsDialog();
                }

                @Override
                public void onProviderDisabled(String provider) {
                    // Location services are disabled, prompt the user to enable them
                    showLocationSettingsDialog();
                }
            });
        }
    }
    private void showLocationName(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String locationName = address.getAddressLine(0); // You can customize this based on your needs
                showToast("Current Location: " + locationName);
                sourceEditText.setText(locationName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}