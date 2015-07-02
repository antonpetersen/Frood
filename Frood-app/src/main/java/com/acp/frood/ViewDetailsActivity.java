package com.acp.frood;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;


import org.w3c.dom.Text;

import java.text.ParseException;
import java.util.List;
import java.util.Queue;


public class ViewDetailsActivity extends Activity {

    FroodEvent fe;
    int AttendeeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_details);
        String id = getIntent().getStringExtra("eventID");
        Log.d("debug", id);
        // Briefly display event id in main textview before loading the actual content view
        TextView test = (TextView) findViewById(R.id.content_view);
        test.setText(id);

        // Get the text to display in the content view
        ParseQuery<FroodEvent> query = ParseQuery.getQuery("Events");
        query.getInBackground(id, new GetCallback<FroodEvent>() {
            @Override
            public void done(FroodEvent event, com.parse.ParseException e) {
                if (e == null) {
                    TextView test = (TextView) findViewById(R.id.content_view);
                    test.setText(event.getText());
                    fe = event;

                    // Set attendeeCount
                    ParseQuery<ParseObject> attendeeCountQuery = ParseQuery.getQuery("Attending");
                    attendeeCountQuery.whereEqualTo("event", fe);
                    attendeeCountQuery.countInBackground(new CountCallback() {
                        @Override
                        public void done(int count, com.parse.ParseException e) {
                            if (e == null) {
                                // The count request succeeded. Set the counter
                                TextView tv = (TextView) findViewById(R.id.attendee_count);
                                tv.setText("Attendees: " + Integer.toString(count));
                                Log.d("Debug", "Attendee count set");
                                AttendeeCount = count;
                            }
                        }
                    });

                    // Set Toggle Button status
                    ParseQuery<ParseObject> attendQuery = ParseQuery.getQuery("Attending");
                    attendQuery.whereEqualTo("user", ParseUser.getCurrentUser());
                    attendQuery.whereEqualTo("event", fe);
                    attendQuery.getFirstInBackground(new GetCallback<ParseObject>() {
                        @Override
                        public void done(ParseObject parseObject, com.parse.ParseException e) {
                            ToggleButton tb = (ToggleButton) findViewById(R.id.attend_btn);
                            tb.setEnabled(true);
                            if (parseObject != null) {
                                tb.setChecked(true);
                                Log.d("Debug", "Toggle Button status set");
                            }

                        }
                    });
                } else {
                    // something went wrong
                    Log.d("Debug", "Failed to set Toggle Button status");
                }
            }
        });



    }


    // Toggle Button Handler
    public void ToggleAttend(View view) {
        if (((ToggleButton) view).isChecked()){
            ParseObject attend = new ParseObject("Attending");
            attend.put("user", ParseUser.getCurrentUser());
            attend.put("event", fe);
            attend.saveInBackground();

            // Refresh attendeeCount
            TextView tv = (TextView) findViewById(R.id.attendee_count);
            AttendeeCount = AttendeeCount +1;
            tv.setText("Attendees: " + AttendeeCount);
            Log.d("Debug", "Attendee count incremented");


        }else{
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Attending");
            query.whereEqualTo("user", ParseUser.getCurrentUser());
            query.whereEqualTo("event", fe);
            query.getFirstInBackground(new GetCallback<ParseObject>(){
                @Override
                public void done(ParseObject parseObject, com.parse.ParseException e) {
            parseObject.deleteInBackground();

            // Refresh attendeeCount
            TextView tv = (TextView) findViewById(R.id.attendee_count);
            AttendeeCount = AttendeeCount -1;
            tv.setText("Attendees: " + AttendeeCount);
            Log.d("Debug", "Attendee count decremented");

                }
            });
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
