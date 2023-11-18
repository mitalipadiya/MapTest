package com.example.maptest.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

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
    private static boolean isFirstLocationUpdate = true;
    private AutoCompleteTextView sourceEditText, destinationEditText;
    private static final String  OPENCAGE_API_KEY  = "ce0b8aa59a7d44ebb30298a06a04cbdc";
    static ArrayAdapter<String> sourceAdapter;
    static ArrayAdapter<String> destinationAdapter;

    private Marker destinationMarker;
    private Polyline routePolyline;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mThis = getContext();
        mActivity = getActivity();
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        sourceEditText = rootView.findViewById(R.id.sourceEditText);
        destinationEditText = rootView.findViewById(R.id.destinationEditText);

        // Set up the adapter for autocomplete suggestions
        sourceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line);
        destinationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line);

        sourceEditText.setAdapter(sourceAdapter);
        destinationEditText.setAdapter(destinationAdapter);

        // Add a TextChangedListener to fetch suggestions as the user types
        sourceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Fetch suggestions as the user types
                if(charSequence.toString().length() >= 3) {
                    fetchLocationSuggestions(charSequence.toString(), true);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        // Add a TextChangedListener to fetch suggestions as the user types
        destinationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Fetch suggestions as the user types
                if(charSequence.toString().length() >= 3) {
                    fetchLocationSuggestions(charSequence.toString(), false);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        sourceEditText.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected item from the adapter
            String selectedItem = (String) parent.getItemAtPosition(position);

            // Use the geocoder to get the details (latitude, longitude) for the selected item
            try {
                List<Address> addresses = geocoder.getFromLocationName(selectedItem, 1);
                if (addresses != null && addresses.size() > 0) {
                    Address selectedAddress = addresses.get(0);
                    double selectedLatitude = selectedAddress.getLatitude();
                    double selectedLongitude = selectedAddress.getLongitude();

                    // Now you have the name (selectedItem), latitude, and longitude
                    showToast("Selected Location: " + selectedItem +
                            "\nLatitude: " + selectedLatitude +
                            "\nLongitude: " + selectedLongitude);
                    // Update the marker on the map with the selected location
                    updateMarker(new LatLng(selectedLatitude, selectedLongitude), selectedItem, true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        destinationEditText.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected item from the adapter
            String selectedItem = (String) parent.getItemAtPosition(position);

            // Use the geocoder to get the details (latitude, longitude) for the selected item
            try {
                List<Address> addresses = geocoder.getFromLocationName(selectedItem, 1);
                if (addresses != null && addresses.size() > 0) {
                    Address selectedAddress = addresses.get(0);
                    double selectedLatitude = selectedAddress.getLatitude();
                    double selectedLongitude = selectedAddress.getLongitude();

                    // Now you have the name (selectedItem), latitude, and longitude
                    showToast("Selected Location: " + selectedItem +
                            "\nLatitude: " + selectedLatitude +
                            "\nLongitude: " + selectedLongitude);
                    // Update the marker on the map with the selected location
                    updateMarker(new LatLng(selectedLatitude, selectedLongitude), selectedItem, false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        // Check and request location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestLocationPermissions();
        } else {
            // Permissions are already granted for devices below Android 6.0
            initMap();
        }

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return rootView;
    }
    private void updateMarker(LatLng latLng, String locationName, boolean isSource) {
        // Remove the previous marker if exists
        if (isSource) {
            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
            }
            // Add a marker for the source (current location)
            currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(locationName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            if (destinationMarker != null) {
                destinationMarker.remove();
            }
            // Add a marker for the destination
            destinationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(locationName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
        // Move the camera to the marker's position
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        // Update source marker
        if (currentLocationMarker != null && destinationMarker != null) {
            drawPolyline();
        }
    }
    private void drawPolyline() {
        if (routePolyline != null) {
            routePolyline.remove(); // Remove previous polyline
        }

        // Draw a polyline between source and destination
        LatLng sourceLatLng = currentLocationMarker.getPosition();
        LatLng destinationLatLng = destinationMarker.getPosition();

        PolylineOptions polylineOptions = new PolylineOptions()
                .add(sourceLatLng, destinationLatLng)
                .width(5) // Set the width of the polyline
                .color(ContextCompat.getColor(requireContext(), R.color.teal_200)); // Set the color

        routePolyline = googleMap.addPolyline(polylineOptions);
    }
    private void fetchLocationSuggestions(String input, boolean isSource) {
        // Perform the Geocoding API request in a background thread
        new FetchLocationSuggestionsTask(isSource).execute(input);
    }

    private static class FetchLocationSuggestionsTask extends AsyncTask<String, Void, ArrayList<String>> {
        boolean isSource;
        FetchLocationSuggestionsTask(boolean isSource) {
            this.isSource = isSource;
        }
        @Override
        protected ArrayList<String> doInBackground(String... params) {
            ArrayList<String> suggestions = new ArrayList<>();

            try {
                // Construct the URL for the Geocoding API request
                String apiUrl = "https://api.opencagedata.com/geocode/v1/json" +
                        "?q=" + URLEncoder.encode(params[0], "UTF-8") +
                        "&key=" + OPENCAGE_API_KEY + "&countrycode=ca";

                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String response = readStream(in);

                    // Parse the response and extract suggestions
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray results = jsonResponse.getJSONArray("results");

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        String address = result.getString("formatted");
                        Log.i("Mapdata ==>", address);
                        suggestions.add(address);
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return suggestions;
        }

        @Override
        protected void onPostExecute(ArrayList<String> suggestions) {
            // Update the adapter with the fetched suggestions
            if(isSource) {
                sourceAdapter.clear();
                sourceAdapter.addAll(suggestions);
                sourceAdapter.notifyDataSetChanged();
            } else {
                destinationAdapter.clear();
                destinationAdapter.addAll(suggestions);
                destinationAdapter.notifyDataSetChanged();
            }
        }
    }

    private static String readStream(InputStream is) {
        try (Scanner s = new Scanner(is).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
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
            initMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get the location
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
                    if (isFirstLocationUpdate && currentLocationMarker == null) {
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