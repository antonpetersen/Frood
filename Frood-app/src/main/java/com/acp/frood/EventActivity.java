package com.acp.frood;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.SaveCallback;

/**
 * Activity which displays a login screen to the user, offering registration as well.
 */
public class EventActivity extends Activity {
  // UI references.
  private EditText eventEditText;
  private TextView characterCountTextView;
  private Button eventButton;

  private int maxCharacterCount = Application.getConfigHelper().getEventMaxCharacterCount();
  private ParseGeoPoint geoPoint;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_event);

    Intent intent = getIntent();
    Location location = intent.getParcelableExtra(Application.INTENT_EXTRA_LOCATION);
    geoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

    eventEditText = (EditText) findViewById(R.id.event_edittext);
    eventEditText.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            updateEventButtonState();
            updateCharacterCountTextViewText();
        }
    });

    characterCountTextView = (TextView) findViewById(R.id.character_count_textview);

    eventButton = (Button) findViewById(R.id.event_button);
    eventButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            event();
        }
    });

    updateEventButtonState();
    updateCharacterCountTextViewText();
  }

  private void event() {
    final String text = eventEditText.getText().toString().trim();

    // Set up a progress dialog
    final ProgressDialog dialog = new ProgressDialog(EventActivity.this);
    dialog.setMessage(getString(R.string.progress_event));
    dialog.show();

    // Create a event.
    FroodEvent event = new FroodEvent();



    // Set the location to the current user's location
    event.setLocation(geoPoint);
    event.setText(text);
    event.setUser(ParseUser.getCurrentUser());
    ParseACL acl = new ParseACL();

    // Give public read access
    acl.setPublicReadAccess(true);
    event.setACL(acl);

      // Set event creator to attend the "event"/event
//    event.add("attending", (event.get("user")));

      //


     // [{"__type":"Pointer","className":"_User","objectId":"uccQVO7eSb"}]



    // Save the event
    event.saveInBackground(new SaveCallback() {
        @Override
        public void done(ParseException e) {
            dialog.dismiss();
            Log.d("Saved???", "maybe");
            finish();
            // TODO Set CountDownTimer


        }
    });
    // TODO Move logic to server side - to avoid users spamming - https://www.parse.com/docs/android/guide#push-notifications
    // Push the event
    ParsePush push = new ParsePush();
    push.setChannel("DTU");
    push.setMessage(text);
    push.sendInBackground();
    Log.d("rofl push", "apps");
  }

  private String getEventEditTextText() {
    return eventEditText.getText().toString().trim();
  }

  private void updateEventButtonState() {
    int length = getEventEditTextText().length();
    boolean enabled = length > 0 && length < maxCharacterCount;
    eventButton.setEnabled(enabled);
  }

  private void updateCharacterCountTextViewText () {
    String characterCountString = String.format("%d/%d", eventEditText.length(), maxCharacterCount);
    characterCountTextView.setText(characterCountString);
  }
}
