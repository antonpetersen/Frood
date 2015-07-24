package com.acp.frood;

import android.content.Context;
import android.content.SharedPreferences;


import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;


public class Application extends android.app.Application {
  // Debugging switch
  public static final boolean APPDEBUG = true;

  // Debugging tag for the application
  public static final String APPTAG = "Frood";

  // Used to pass location from MainActivity to EventActivity
  public static final String INTENT_EXTRA_LOCATION = "location";

  private static SharedPreferences preferences;

  private static ConfigHelper configHelper;

  public Application() {
  }

  @Override
  public void onCreate() {

    super.onCreate();

    ParseObject.registerSubclass(FroodEvent.class);
    Parse.initialize(this, "dORM0gNF0KpQknqoNHo004rozVcIMs1j3iRIMAgm",
            "aW4WJmenNxWk5gplNpuvPVYy9zUBZssu6NjAnwZc");
    ParseInstallation.getCurrentInstallation().saveInBackground();

    preferences = getSharedPreferences("com.acp.frood", Context.MODE_PRIVATE);

    configHelper = new ConfigHelper();
    configHelper.fetchConfigIfNeeded();

      ParsePush.subscribeInBackground("DTU");

  }

  public static ConfigHelper getConfigHelper() {
    return configHelper;
  }


}
