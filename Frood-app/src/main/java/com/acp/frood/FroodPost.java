package com.acp.frood;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Date;

/**
 * Data model for a post.
 */
@ParseClassName("Posts")
public class FroodPost extends ParseObject {
  public String getText() {
    return getString("text");
  }

  public void setText(String value) {
    put("text", value);
  }

  public ParseUser getUser() {
    return getParseUser("user");
  }

    //TODO add post.getCreatedAt method


  public void setUser(ParseUser value) {
    put("user", value);
  }

  public ParseGeoPoint getLocation() {
    return getParseGeoPoint("location");
  }

  public void setLocation(ParseGeoPoint value) {
    put("location", value);
  }

  public static ParseQuery<FroodPost> getQuery() {
    return ParseQuery.getQuery(FroodPost.class);
  }
}
