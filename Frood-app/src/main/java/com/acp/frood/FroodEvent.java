package com.acp.frood;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/**
 * Data model for an event.
 */
@ParseClassName("Events")
public class FroodEvent extends ParseObject {
  public String getText() {
    return getString("text");
  }

  public void setText(String value) {
    put("text", value);
    put("attendees", 0);
  }


  public ParseUser getUser() {
    return getParseUser("user");
  }

  public void setUser(ParseUser value) {
    put("user", value);
  }

  public ParseGeoPoint getLocation() {
    return getParseGeoPoint("location");
  }

  public void setLocation(ParseGeoPoint value) {
    put("location", value);
  }

  public static ParseQuery<FroodEvent> getQuery() {
    return ParseQuery.getQuery(FroodEvent.class);
  }
}
