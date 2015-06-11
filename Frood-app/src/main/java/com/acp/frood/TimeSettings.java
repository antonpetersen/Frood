package com.acp.frood;

import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.TimePicker;
import android.widget.Toast;

/**
 * Created by anton on 11/06/15.
 */
public class TimeSettings implements TimePickerDialog.OnTimeSetListener{
    Context context;
    public TimeSettings(Context context)
    {
        this.context = context;

    }
    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Toast.makeText(context,"Selected time is hour :"+hourOfDay+" minute :"+minute,Toast.LENGTH_LONG).show();
    }
}
