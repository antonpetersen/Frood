package com.acp.frood;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class MainActivity extends FragmentActivity implements LocationListener,
    GooglePlayServicesClient.ConnectionCallbacks,
    GooglePlayServicesClient.OnConnectionFailedListener {

  /*
   * Define a request code to send to Google Play services This code is returned in
   * Activity.onActivityResult
   */
  private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

  /*
   * Constants for location update parameters
   */

  // Update interval in milliseconds
  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000 * 5;

  // A fast ceiling of update intervals, used when the app is visible
  private static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS = 1000;

  /*
   * Constants for handling location results
   */

  // Initial offset for calculating the map bounds
  private static final double OFFSET_CALCULATION_INIT_DIFF = 1.0;

  // Accuracy for calculating the map bounds
  private static final float OFFSET_CALCULATION_ACCURACY = 0.01f;



  /*
   * Other class member variables
   */
  // Map fragment
  private SupportMapFragment mapFragment;

  // Represents the circle around a map
  private Circle mapCircle;

  // Fields for the map radius in feet
  private float radius;
  private float lastRadius;

  // Fields for helping process map and location changes
  private final Map<String, Marker> mapMarkers = new HashMap<String, Marker>();
  private int mostRecentMapUpdate;
  private boolean hasSetUpInitialLocation;
  private String selectedEventObjectId;
  private Location lastLocation = new Location("");
  private Location currentLocation;

  // A request to connect to Location Services
  private LocationRequest locationRequest;

  // Stores the current instantiation of the location client in this object
  private LocationClient locationClient;

  // Adapter for the Parse query
  private ParseQueryAdapter<FroodEvent> eventsQueryAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    radius = 5;
    lastRadius = radius;
    setContentView(R.layout.activity_main);

    // Create a new global location parameters object
    locationRequest = LocationRequest.create();

    // Set the update interval
    locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

    // Use high accuracy
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    // Set the interval ceiling to one minute
    locationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS);

    // Create a new location client, using the enclosing class to handle callbacks.
    locationClient = new LocationClient(this, this, this);

    // Default location (DTU, Denmark)
    lastLocation.setLatitude(55.78402);
    lastLocation.setLongitude(12.52029);

    // Set up a customized query
    ParseQueryAdapter.QueryFactory<FroodEvent> factory =
        new ParseQueryAdapter.QueryFactory<FroodEvent>() {
          public ParseQuery<FroodEvent> create() {
            ParseQuery<FroodEvent> query = FroodEvent.getQuery();
            query.include("user");
            query.orderByDescending("createdAt");
            query.setLimit(1000);
            return query;
          }
        };


    // Set up the query adapter
    eventsQueryAdapter = new ParseQueryAdapter<FroodEvent>(this, factory) {
      @Override
      public View getItemView(final FroodEvent event, View view, ViewGroup parent) {
        if (view == null) {
          view = View.inflate(getContext(), R.layout.frood_event_item, null);
        }
        TextView contentView = (TextView) view.findViewById(R.id.content_view);
        TextView usernameView = (TextView) view.findViewById(R.id.username_view);
        TextView createdAtView = (TextView) view.findViewById(R.id.createdat_view);
        Button viewDetailsView = (Button) view.findViewById(R.id.view_details_btn);
        contentView.setText(event.getText());
        usernameView.setText("Shared by: " + event.getUser().getUsername());

        //Get time elapsed since creation
          DateTime createdAt = new DateTime(event.getCreatedAt());
          DateTime now = new DateTime();
          Period period = new Period(createdAt, now);

          PeriodFormatter formatter = new PeriodFormatterBuilder()
                  .appendMinutes().appendSuffix(" minutes ")
                  .appendHours().appendSuffix(" hours ")
                  .appendDays().appendSuffix(" days ")
                  .printZeroNever()
                  .toFormatter();

          String elapsed = formatter.print(period);
          createdAtView.setText("Meal shared for:\n" + elapsed);

          viewDetailsView.setOnClickListener(new OnClickListener() {
              public void onClick(View v) {
                  Log.d("debug", event.getText());
                  Intent intent = new Intent(MainActivity.this, ViewDetailsActivity.class);
                  intent.putExtra("eventID", event.getObjectId());
                  startActivity(intent);
              }
          });

        return view;
      }
    };

    // Disable automatic loading when the adapter is attached to a view.
    eventsQueryAdapter.setAutoload(false);

    // Disable pagination, we'll manage the query limit ourselves
    eventsQueryAdapter.setPaginationEnabled(false);

    // Attach the query adapter to the view
    ListView eventsListView = (ListView) findViewById(R.id.events_listview);
    eventsListView.setAdapter(eventsQueryAdapter);

    // Set up the handler for an item's selection
    eventsListView.setOnItemClickListener(new OnItemClickListener() {

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final FroodEvent item = eventsQueryAdapter.getItem(position);
            selectedEventObjectId = item.getObjectId();
            mapFragment.getMap().animateCamera(
                    CameraUpdateFactory.newLatLng(new LatLng(item.getLocation().getLatitude(), item
                            .getLocation().getLongitude())), new CancelableCallback() {
                        public void onFinish() {
                            Marker marker = mapMarkers.get(item.getObjectId());
                            if (marker != null) {
                                marker.showInfoWindow();
                            }
                        }

                        public void onCancel() {
                        }
                    });
            Marker marker = mapMarkers.get(item.getObjectId());
            if (marker != null) {
                marker.showInfoWindow();
            }
        }
    });

    // Set up the map fragment
    mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
    // Enable the current location "blue dot"
    mapFragment.getMap().setMyLocationEnabled(true);
    mapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(55.786096, 10.736059), 5.3f));
    // Set up the camera change handler
    mapFragment.getMap().setOnCameraChangeListener(new OnCameraChangeListener() {
        public void onCameraChange(CameraPosition position) {
            // When the camera changes, update the query
            doMapQuery();
        }
    });

    // Set up the handler for the event button click
    final Button eventButton = (Button) findViewById(R.id.event_button);
    eventButton.setEnabled(true);
    eventButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {

            // Only allow events if we have a location
            Location myLoc = (currentLocation == null) ? lastLocation : currentLocation;
            if (myLoc == null) {
                Toast.makeText(MainActivity.this,
                        "Please try again after your location appears on the map.", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, EventActivity.class);
            intent.putExtra(Application.INTENT_EXTRA_LOCATION, myLoc);
            startActivity(intent);
        }
    });


  }

  /*
   * Called when the Activity is no longer visible at all. Stop updates and disconnect.
   */
  @Override
  public void onStop() {
    // If the client is connected
    if (locationClient.isConnected()) {
      stopPeriodicUpdates();
    }

    // After disconnect() is called, the client is considered "dead".
    locationClient.disconnect();

    super.onStop();
  }

  /*
   * Called when the Activity is restarted, even before it becomes visible.
   */
  @Override
  public void onStart() {
    super.onStart();

    // Connect to the location services client
    locationClient.connect();
  }

  /*
   * Called when the Activity is resumed. Updates the view.
   */
  @Override
  protected void onResume() {
    super.onResume();

    Application.getConfigHelper().fetchConfigIfNeeded();

    // Get the latest search distance preference
    radius = 5;
    // Checks the last saved location to show cached data if it's available
    if (lastLocation != null) {
      LatLng myLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
      // If the search distance preference has been changed, move
      // map to new bounds.
      if (lastRadius != radius) {
        updateZoom(myLatLng);
      }
      // Update the circle map
      updateCircle(myLatLng);
    }
    // Save the current radius
    lastRadius = radius;
    // Query for the latest data to update the views.
    doMapQuery();
    doListQuery();
  }

  /*
   * Handle results returned to this Activity by other Activities started with
   * startActivityForResult(). In particular, the method onConnectionFailed() in
   * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to start
   * an Activity that handles Google Play services problems. The result of this call returns here,
   * to onActivityResult.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    // Choose what to do based on the request code
    switch (requestCode) {

      // If the request code matches the code sent in onConnectionFailed
      case CONNECTION_FAILURE_RESOLUTION_REQUEST:

        switch (resultCode) {
          // If Google Play services resolved the problem
          case Activity.RESULT_OK:

            if (Application.APPDEBUG) {
              // Log the result
              Log.d(Application.APPTAG, "Connected to Google Play services");
            }

            break;

          // If any other result was returned by Google Play services
          default:
            if (Application.APPDEBUG) {
              // Log the result
              Log.d(Application.APPTAG, "Could not connect to Google Play services");
            }
            break;
        }

        // If any other request code was received
      default:
        if (Application.APPDEBUG) {
          // Report that this Activity received an unknown requestCode
          Log.d(Application.APPTAG, "Unknown request code received for the activity");
        }
        break;
    }
  }

  /*
   * Verify that Google Play services is available before making a request.
   * 
   * @return true if Google Play services is available, otherwise false
   */
  private boolean servicesConnected() {
    // Check that Google Play services is available
    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

    // If Google Play services is available
    if (ConnectionResult.SUCCESS == resultCode) {
      if (Application.APPDEBUG) {
        // In debug mode, log the status
        Log.d(Application.APPTAG, "Google play services available");
      }
      // Continue
      return true;
      // Google Play services was not available for some reason
    } else {
      // Display an error dialog
      Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
      if (dialog != null) {
        ErrorDialogFragment errorFragment = new ErrorDialogFragment();
        errorFragment.setDialog(dialog);
        errorFragment.show(getSupportFragmentManager(), Application.APPTAG);
      }
      return false;
    }
  }

  /*
   * Called by Location Services when the request to connect to the client finishes successfully. At
   * this point, you can request the current location or start periodic updates
   */
  public void onConnected(Bundle bundle) {
    if (Application.APPDEBUG) {
      Log.d("Loc. services connected", Application.APPTAG);
    }
    currentLocation = getLocation();
    startPeriodicUpdates();
  }

  /*
   * Called by Location Services if the connection to the location client drops because of an error.
   */
  public void onDisconnected() {
    if (Application.APPDEBUG) {
      Log.d("Loc. service disconnect", Application.APPTAG);
    }
  }

  /*
   * Called by Location Services if the attempt to Location Services fails.
   */
  public void onConnectionFailed(ConnectionResult connectionResult) {
    // Google Play services can resolve some errors it detects. If the error has a resolution, try
    // sending an Intent to start a Google Play services activity that can resolve error.
    if (connectionResult.hasResolution()) {
      try {

        // Start an Activity that tries to resolve the error
        connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

      } catch (IntentSender.SendIntentException e) {

        if (Application.APPDEBUG) {
          // Thrown if Google Play services canceled the original PendingIntent
          Log.d(Application.APPTAG, "An error occurred when connecting to location services.", e);
        }
      }
    } else {
      // If no resolution is available, display a dialog to the user with the error.
      showErrorDialog(connectionResult.getErrorCode());
    }
  }

  /*
   * Report location updates to the UI.
   */
  public void onLocationChanged(Location location) {
    currentLocation = location;
    if (lastLocation != null
        && geoPointFromLocation(location)
        .distanceInKilometersTo(geoPointFromLocation(lastLocation)) < 0.01) {
      // If the location hasn't changed by more than 10 meters, ignore it.
      return;
    }
    lastLocation = location;
    LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    if (!hasSetUpInitialLocation) {
      // Zoom to the current location.
      updateZoom(myLatLng);
      hasSetUpInitialLocation = true;
    }
    // Update map radius indicator
    updateCircle(myLatLng);
    doMapQuery();
    doListQuery();
  }

  /*
   * In response to a request to start updates, send a request to Location Services
   */
  private void startPeriodicUpdates() {
    locationClient.requestLocationUpdates(locationRequest, this);
  }

  /*
   * In response to a request to stop updates, send a request to Location Services
   */
  private void stopPeriodicUpdates() {
    locationClient.removeLocationUpdates(this);
  }

  /*
   * Get the current location
   */
  private Location getLocation() {
    // If Google Play Services is available
    if (servicesConnected()) {
      // Get the current location
      return locationClient.getLastLocation();
    } else {
      return null;
    }
  }

  /*
   * Set up a query to update the list view
   */
  private void doListQuery() {
    Location myLoc = (currentLocation == null) ? lastLocation : currentLocation;
    // If location info is available, load the data
    if (myLoc != null) {
      // Refreshes the list view with new data based
      // usually on updated location data.
      eventsQueryAdapter.loadObjects();
    }
  }

  /*
   * Set up the query to update the map view
   */
  private void doMapQuery() {
    final int myUpdateNumber = ++mostRecentMapUpdate;
    Location myLoc = (currentLocation == null) ? lastLocation : currentLocation;
    // If location info isn't available, clean up any existing markers
    if (myLoc == null) {
      cleanUpMarkers(new HashSet<String>());
      return;
    }
    final ParseGeoPoint myPoint = geoPointFromLocation(myLoc);
    // Create the map Parse query
    ParseQuery<FroodEvent> mapQuery = FroodEvent.getQuery();
    // Set up additional query filters
    mapQuery.whereWithinKilometers("location", myPoint, 1000);
    mapQuery.include("user");
      //mapQuery.include("createdAt");
    mapQuery.orderByDescending("createdAt");
    mapQuery.setLimit(1000);
    // Kick off the query in the background
    mapQuery.findInBackground(new FindCallback<FroodEvent>() {
      @Override
      public void done(List<FroodEvent> objects, ParseException e) {
        if (e != null) {
          if (Application.APPDEBUG) {
            Log.d(Application.APPTAG, "An error occurred while querying for map events.", e);
          }
          return;
        }
        /*
         * Make sure we're processing results from
         * the most recent update, in case there
         * may be more than one in progress.
         */
        if (myUpdateNumber != mostRecentMapUpdate) {
          return;
        }
        // Events to show on the map
        Set<String> toKeep = new HashSet<String>();
        // Loop through the results of the search
        for (FroodEvent event : objects) {
          // Add this event to the list of map pins to keep
          toKeep.add(event.getObjectId());
          // Check for an existing marker for this event
          Marker oldMarker = mapMarkers.get(event.getObjectId());
          // Set up the map marker's location
          MarkerOptions markerOpts =
              new MarkerOptions().position(new LatLng(event.getLocation().getLatitude(), event
                  .getLocation().getLongitude()));
          // Set up the marker properties based on if it is within the search radius
           // Check for an existing in range marker
            if (oldMarker != null) {
              if (oldMarker.getSnippet() != null) {
                // In range marker already exists, skip adding it
                continue;
              } else {
                // Marker now in range, needs to be refreshed
                oldMarker.remove();
              }
            }

            // Display a green marker with the event information
            markerOpts =
                markerOpts.title(event.getText()).snippet(event.getUser().getUsername())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

          // Add a new marker
          Marker marker = mapFragment.getMap().addMarker(markerOpts);
          mapMarkers.put(event.getObjectId(), marker);
          if (event.getObjectId().equals(selectedEventObjectId)) {
            marker.showInfoWindow();
            selectedEventObjectId = null;
          }
        }
        // Clean up old markers.
        cleanUpMarkers(toKeep);
      }
    });
  }

  /*
   * Helper method to clean up old markers
   */
  private void cleanUpMarkers(Set<String> markersToKeep) {
    for (String objId : new HashSet<String>(mapMarkers.keySet())) {
      if (!markersToKeep.contains(objId)) {
        Marker marker = mapMarkers.get(objId);
        marker.remove();
        mapMarkers.get(objId).remove();
        mapMarkers.remove(objId);
      }
    }
  }

  /*
   * Helper method to get the Parse GEO point representation of a location
   */
  private ParseGeoPoint geoPointFromLocation(Location loc) {
    return new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());
  }

  /*
   * Displays a circle on the map representing the search radius
   */
  private void updateCircle(LatLng myLatLng) {
    if (mapCircle == null) {
      mapCircle =
          mapFragment.getMap().addCircle(
              new CircleOptions().center(myLatLng).radius(radius * 1000));
  int baseColor = Color.WHITE;                                                                        //was ".DKGRAY"
      mapCircle.setStrokeColor(baseColor);
      mapCircle.setStrokeWidth(0);                                                                     //was "2"
      mapCircle.setFillColor(Color.argb(50, Color.red(baseColor), Color.green(baseColor),
          Color.blue(baseColor)));
    }
    mapCircle.setCenter(myLatLng);
    mapCircle.setRadius(radius * 1000); // Convert radius in feet to meters.
  }

  /*
   * Zooms the map to show the area of interest based on the search radius
   */
  private void updateZoom(LatLng myLatLng) {
    // Get the bounds to zoom to
    LatLngBounds bounds = calculateBoundsWithCenter(myLatLng);
    // Zoom to the given bounds
    mapFragment.getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 5));
  }

  /*
   * Helper method to calculate the offset for the bounds used in map zooming
   */
  private double calculateLatLngOffset(LatLng myLatLng, boolean bLatOffset) {
    // The return offset, initialized to the default difference
    double latLngOffset = OFFSET_CALCULATION_INIT_DIFF;
    // Set up the desired offset distance in meters
    float desiredOffsetInMeters = radius * 1000;
    // Variables for the distance calculation
    float[] distance = new float[1];
    boolean foundMax = false;
    double foundMinDiff = 0;
    // Loop through and get the offset
    do {
      // Calculate the distance between the point of interest
      // and the current offset in the latitude or longitude direction
      if (bLatOffset) {
        Location.distanceBetween(myLatLng.latitude, myLatLng.longitude, myLatLng.latitude
            + latLngOffset, myLatLng.longitude, distance);
      } else {
        Location.distanceBetween(myLatLng.latitude, myLatLng.longitude, myLatLng.latitude,
            myLatLng.longitude + latLngOffset, distance);
      }
      // Compare the current difference with the desired one
      float distanceDiff = distance[0] - desiredOffsetInMeters;
      if (distanceDiff < 0) {
        // Need to catch up to the desired distance
        if (!foundMax) {
          foundMinDiff = latLngOffset;
          // Increase the calculated offset
          latLngOffset *= 2;
        } else {
          double tmp = latLngOffset;
          // Increase the calculated offset, at a slower pace
          latLngOffset += (latLngOffset - foundMinDiff) / 2;
          foundMinDiff = tmp;
        }
      } else {
        // Overshot the desired distance
        // Decrease the calculated offset
        latLngOffset -= (latLngOffset - foundMinDiff) / 2;
        foundMax = true;
      }
    } while (Math.abs(distance[0] - desiredOffsetInMeters) > OFFSET_CALCULATION_ACCURACY);
    return latLngOffset;
  }

  /*
   * Helper method to calculate the bounds for map zooming
   */
  LatLngBounds calculateBoundsWithCenter(LatLng myLatLng) {
    // Create a bounds
    LatLngBounds.Builder builder = LatLngBounds.builder();

    // Calculate east/west points that should to be included
    // in the bounds
    double lngDifference = calculateLatLngOffset(myLatLng, false);
    LatLng east = new LatLng(myLatLng.latitude, myLatLng.longitude + lngDifference);
    builder.include(east);
    LatLng west = new LatLng(myLatLng.latitude, myLatLng.longitude - lngDifference);
    builder.include(west);

    // Calculate north/south points that should to be included
    // in the bounds
    double latDifference = calculateLatLngOffset(myLatLng, true);
    LatLng north = new LatLng(myLatLng.latitude + latDifference, myLatLng.longitude);
    builder.include(north);
    LatLng south = new LatLng(myLatLng.latitude - latDifference, myLatLng.longitude);
    builder.include(south);

    return builder.build();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);

    menu.findItem(R.id.action_settings).setOnMenuItemClickListener(new OnMenuItemClickListener() {
      public boolean onMenuItemClick(MenuItem item) {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        return true;
      }
    });
    return true;
  }

  /*
   * Show a dialog returned by Google Play services for the connection error code
   */
  private void showErrorDialog(int errorCode) {
    // Get the error dialog from Google Play services
    Dialog errorDialog =
        GooglePlayServicesUtil.getErrorDialog(errorCode, this,
            CONNECTION_FAILURE_RESOLUTION_REQUEST);

    // If Google Play services can provide an error dialog
    if (errorDialog != null) {

      // Create a new DialogFragment in which to show the error dialog
      ErrorDialogFragment errorFragment = new ErrorDialogFragment();

      // Set the dialog in the DialogFragment
      errorFragment.setDialog(errorDialog);

      // Show the error dialog in the DialogFragment
      errorFragment.show(getSupportFragmentManager(), Application.APPTAG);
    }
  }

  /*
   * Define a DialogFragment to display the error dialog generated in showErrorDialog.
   */
  public static class ErrorDialogFragment extends DialogFragment {
    // Global field to contain the error dialog
    private Dialog mDialog;

    /**
     * Default constructor. Sets the dialog field to null
     */
    public ErrorDialogFragment() {
      super();
      mDialog = null;
    }

    /*
     * Set the dialog to display
     * 
     * @param dialog An error dialog
     */
    public void setDialog(Dialog dialog) {
      mDialog = dialog;
    }

    /*
     * This method must return a Dialog to the DialogFragment.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      return mDialog;
    }
  }
}
