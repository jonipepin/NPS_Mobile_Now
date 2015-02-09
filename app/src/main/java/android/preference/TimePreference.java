/* Custom Time Picker Preference
 * Version found at source below, modified by J. Pepin to add functionality
 * used to implement a Time dialog box as a preference item for Preference Activity
 * ref: http://stackoverflow.com/questions/5533078/timepicker-in-preferencescreen
 */

package android.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference {
    private int lastHour=0;
    private int lastMinute=0;
    private TimePicker picker=null;

    public static int getHour(String time) {
        String[] pieces=time.split(":");

        return(Integer.parseInt(pieces[0]));
    }

    public static int getMinute(String time) {
        String[] pieces=time.split(":");

        return(Integer.parseInt(pieces[1]));
    }
       
    public int getHour() {
    	return lastHour;
    }
    
    public int getMinute() {
    	return lastMinute;
    }
    
    public String getTime() {
    	String tempHour = String.valueOf(lastHour);
    	String tempMin = String.valueOf(lastMinute);
    	if (tempHour.length() == 1){
    		tempHour = "0" + tempHour;
    	}
    	if (tempMin.length() == 1){
    		tempMin = "0" + tempMin;
    	}
    	return tempHour+":"+tempMin;
    }

    public TimePreference(Context ctxt) {
        this(ctxt, null);
    }

    public TimePreference(Context ctxt, AttributeSet attrs) {
        this(ctxt, attrs, 0);
    }

    public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);

        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");
    }

    @Override
    protected View onCreateDialogView() {
        picker=new TimePicker(getContext());
        picker.setIs24HourView(true);

        return(picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        picker.setCurrentHour(lastHour);
        picker.setCurrentMinute(lastMinute);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
        	// this is needed for keyboard input to stick.
            // it's a known bug that input will be ignored while the input area 
        	// still has focus:
            // http://groups.google.com/group/android-developers/browse_thread/thread/58ee0bede9ad9b8b?pli=1
        	picker.clearFocus();
        	
            lastHour=picker.getCurrentHour();
            lastMinute=picker.getCurrentMinute();

            String time = getTime();
            

            if (callChangeListener(time)) {
                persistString(time);
            }
        }        
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return(a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time=null;

        if (restoreValue) {
            if (defaultValue==null) {
                time=getPersistedString("00:00");
            }
            else {
                time=getPersistedString(defaultValue.toString());
            }
        }
        else {
            time=defaultValue.toString();
        }

        lastHour=getHour(time);
        lastMinute=getMinute(time);
    }
}