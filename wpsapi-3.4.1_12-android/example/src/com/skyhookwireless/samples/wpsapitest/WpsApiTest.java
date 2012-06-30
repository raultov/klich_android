package com.skyhookwireless.samples.wpsapitest;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skyhookwireless.wps.IPLocation;
import com.skyhookwireless.wps.IPLocationCallback;
import com.skyhookwireless.wps.Location;
import com.skyhookwireless.wps.WPSAuthentication;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSLocationCallback;
import com.skyhookwireless.wps.WPSPeriodicLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.WPSStreetAddressLookup;
import com.skyhookwireless.wps.XPS;

public class WpsApiTest
    extends Activity
    implements OnSharedPreferenceChangeListener
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // create the XPS instance, passing in our Context
        _xps = new XPS(this);
        _stop = false;

        // listen for settings changes
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        // read existing preferences
        onSharedPreferenceChanged(preferences, "Username");
        onSharedPreferenceChanged(preferences, "Realm");
        onSharedPreferenceChanged(preferences, "Local File Path");
        onSharedPreferenceChanged(preferences, "Period");
        onSharedPreferenceChanged(preferences, "Iterations");
        onSharedPreferenceChanged(preferences, "Desired XPS Accuracy");
        onSharedPreferenceChanged(preferences, "Street Address Lookup");
        onSharedPreferenceChanged(preferences, "Tiling Path");
        onSharedPreferenceChanged(preferences, "Max Data Per Session");
        onSharedPreferenceChanged(preferences, "Max Data Total");

        // set the UI layout
        _buttonLayout = new LinearLayout(this);
        _buttonLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(_buttonLayout);

        // initialize the Handler which will display location data
        // in the text view. we use a Handler because UI updates
        // must occur in the UI thread
        setUIHandler();

        // display the buttons.
        // _viewsToDisable is a list of views
        // which should be disabled when WPS is active
        _viewsToDisable.clear();
        _viewsToDisable.add(addSettingsButton(_buttonLayout));
        _viewsToDisable.add(addIPLocationButton(_buttonLayout));
        _viewsToDisable.add(addWPSLocationButton(_buttonLayout));
        _viewsToDisable.add(addWPSPeriodicLocationButton(_buttonLayout));
        _viewsToDisable.add(addXPSLocationButton(_buttonLayout));
        _stopButton = addStopButton(_buttonLayout);
        addAbortButton(_buttonLayout);
        deactivateStopButton();

        // create the location layout
        _tv = new TextView(this);
        _buttonLayout.addView(_tv,
                              new LayoutParams(LayoutParams.FILL_PARENT,
                                               LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        // make sure WPS is stopped
        _xps.abort();
    }

    private LinearLayout _buttonLayout;
    private ArrayList<View> _viewsToDisable = new ArrayList<View>();
    private Button _stopButton;
    private boolean _stop;

    // add the 'Settings' button which leads to all the
    // WPS settings.
    private Button addSettingsButton(ViewGroup layout)
    {
        Button settingsButton = new Button(this);
        settingsButton.setText("Settings");
        settingsButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                Intent launchPreferencesIntent =
                    new Intent().setClass(WpsApiTest.this, Preferences.class);
                startActivity(launchPreferencesIntent);
            }
        });
        layout.addView(settingsButton);
        return settingsButton;
    }

    /**
     * A single callback class that will be used to handle all notifications
     * sent by WPS to our app.
     */
    private class MyLocationCallback
        implements IPLocationCallback,
                   WPSLocationCallback,
                   WPSPeriodicLocationCallback
    {
        public void done()
        {
            // tell the UI thread to re-enable the buttons
            _handler.sendMessage(_handler.obtainMessage(DONE_MESSAGE));
        }

        public WPSContinuation handleError(WPSReturnCode error)
        {
            // send a message to display the error
            _handler.sendMessage(_handler.obtainMessage(ERROR_MESSAGE,
                                                        error));
            // return WPS_STOP if the user pressed the Stop button
            if (! _stop)
                return WPSContinuation.WPS_CONTINUE;
            else
                return WPSContinuation.WPS_STOP;
        }

        public void handleIPLocation(IPLocation location)
        {
            // send a message to display the location
            _handler.sendMessage(_handler.obtainMessage(LOCATION_MESSAGE,
                                                        location));
        }

        public void handleWPSLocation(WPSLocation location)
        {
            // send a message to display the location
            _handler.sendMessage(_handler.obtainMessage(LOCATION_MESSAGE,
                                                        location));
        }

        public WPSContinuation handleWPSPeriodicLocation(WPSLocation location)
        {
            _handler.sendMessage(_handler.obtainMessage(LOCATION_MESSAGE,
                                                        location));
            // return WPS_STOP if the user pressed the Stop button
            if (! _stop)
                return WPSContinuation.WPS_CONTINUE;
            else
                return WPSContinuation.WPS_STOP;
        }
    }

    private final MyLocationCallback _callback = new MyLocationCallback();

    private Button addIPLocationButton(ViewGroup layout)
    {
        Button ipLocationButton = new Button(this);
        ipLocationButton.setText("Get IP Location");

        ipLocationButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                activateStopButton();
                _tv.setText("");
                WPSAuthentication auth =
                    new WPSAuthentication(_username, _realm);
                _xps.getIPLocation(auth,
                                   _streetAddressLookup,
                                   _callback);
            }
        });
        layout.addView(ipLocationButton);
        return ipLocationButton;
    }

    private Button addWPSPeriodicLocationButton(ViewGroup layout)
    {
        Button wpsLocationButton = new Button(this);
        wpsLocationButton.setText("Get WPS Periodic Location");

        wpsLocationButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                activateStopButton();
                _tv.setText("");
                WPSAuthentication auth =
                    new WPSAuthentication(_username, _realm);
                _xps.getPeriodicLocation(auth,
                                         _streetAddressLookup,
                                         _period,
                                         _iterations,
                                         _callback);
            }
        });
        layout.addView(wpsLocationButton);
        return wpsLocationButton;
    }

    private Button addWPSLocationButton(ViewGroup layout)
    {
        Button wpsLocationButton = new Button(this);
        wpsLocationButton.setText("Get WPS Location");

        wpsLocationButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                activateStopButton();
                _tv.setText("");
                WPSAuthentication auth =
                    new WPSAuthentication(_username, _realm);
                _xps.getLocation(auth,
                                 _streetAddressLookup,
                                 _callback);
            }
        });
        layout.addView(wpsLocationButton);
        return wpsLocationButton;
    }

    private Button addXPSLocationButton(ViewGroup layout)
    {
        Button xpsLocationButton = new Button(this);
        xpsLocationButton.setText("Get XPS Location");
        xpsLocationButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                activateStopButton();
                _tv.setText("");
                WPSAuthentication auth =
                    new WPSAuthentication(_username, _realm);
                _xps.getXPSLocation(auth,
                                    // note we convert _period to seconds
                                    (int) (_period / 1000),
                                    _desiredXpsAccuracy,
                                    _callback);
            }
        });
        layout.addView(xpsLocationButton);
        return xpsLocationButton;
    }

    private Button addStopButton(ViewGroup layout)
    {
        final Button stopButton = new Button(this);
        stopButton.setText("Stop");
        stopButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                _stop = true;
                stopButton.setEnabled(false);
            }
        });
        layout.addView(stopButton);
        return stopButton;
    }

    private Button addAbortButton(ViewGroup layout)
    {
        final Button abortButton = new Button(this);
        abortButton.setText("Abort");
        abortButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                _xps.abort();
            }
        });
        layout.addView(abortButton);
        return abortButton;
    }

    private void activateStopButton()
    {
        for (final View view : _viewsToDisable)
        {
            view.setEnabled(false);
        }
        _stopButton.setEnabled(true);
    }

    private void deactivateStopButton()
    {
        for (final View view : _viewsToDisable)
        {
            view.setEnabled(true);
        }
        _stopButton.setEnabled(false);
    }

    // our Handler understands three messages:
    // a location, an error, or a finished request
    private static final int LOCATION_MESSAGE = 1;
    private static final int ERROR_MESSAGE = 2;
    private static final int DONE_MESSAGE = 3;

    private void setUIHandler()
    {
        _handler = new Handler()
        {
            @Override
            public void handleMessage(final Message msg)
            {
                switch (msg.what)
                {
                case LOCATION_MESSAGE:
                    Location location = (Location) msg.obj;
                    _tv.setText(location.toString());
                    return;
                case ERROR_MESSAGE:
                    _tv.setText(((WPSReturnCode) msg.obj).name());
                    return;
                case DONE_MESSAGE:
                    deactivateStopButton();
                    _stop = false;
                }
            }
        };
    }

    /**
     * Preferences management code
     */
    public static class Preferences
        extends PreferenceActivity
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setPreferenceScreen(createRootPreferenceScreen());
        }

        private PreferenceScreen createRootPreferenceScreen()
        {
            final PreferenceScreen root =
                getPreferenceManager().createPreferenceScreen(this);

            final PreferenceCategory category = new PreferenceCategory(this);
            category.setTitle("WpsApiTest Settings");
            root.addPreference(category);

            for (int i = 0; i < options.length;)
            {
                int iIncrement;
                Option optionType = (Option) options[i + 1];
                Preference setting = null;
                if (optionType == Option.TEXT
                        || optionType == Option.NONNEGATIVE_INTEGER)
                {
                    final EditTextPreference textSetting =
                        new EditTextPreference(this);
                    setting = textSetting;
                    textSetting.getEditText().setSingleLine();
                    if (optionType == Option.NONNEGATIVE_INTEGER)
                        textSetting.getEditText()
                                   .setKeyListener(new DigitsKeyListener(false,
                                                                         false));
                    iIncrement = 2;
                }
                else if (optionType == Option.CHECKBOX)
                {
                    final CheckBoxPreference checkSetting =
                        new CheckBoxPreference(this);
                    setting = checkSetting;
                    iIncrement = 2;
                }
                else if (optionType == Option.LIST)
                {
                    final ListPreference listSetting = new ListPreference(this);
                    setting = listSetting;
                    listSetting.setEntries((String[]) options[i+2]);
                    listSetting.setEntryValues((String[]) options[i+2]);
                    iIncrement = 3;
                }
                else
                {
                    iIncrement = 2;
                }
                if (setting != null)
                {
                    setting.setKey((String) options[i]);
                    setting.setTitle((String) options[i]);
                    category.addPreference(setting);
                }
                i += iIncrement;
            }

            return root;
        }

        private enum Option
        {
            TEXT,
            NONNEGATIVE_INTEGER,
            LIST,
            CHECKBOX;
        }

        private static Object[] options =
            new Object[]
                {"Username"             , Option.TEXT,
                 "Realm"                , Option.TEXT,
                 "Local File Path"      , Option.TEXT,
                 "Period"               , Option.NONNEGATIVE_INTEGER,
                 "Iterations"           , Option.NONNEGATIVE_INTEGER,
                 "Desired XPS Accuracy" , Option.NONNEGATIVE_INTEGER,
                 "Tiling Path"          , Option.TEXT,
                 "Max Data Per Session" , Option.NONNEGATIVE_INTEGER,
                 "Max Data Total"       , Option.NONNEGATIVE_INTEGER,
                 "Street Address Lookup", Option.LIST, new String[] {"None", "Limited", "Full"}};

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key)
    {
        if (sharedPreferences.getString(key, "default").equals(""))
        {
            // delete empty preferences so we get the default values below
            final Editor editor = sharedPreferences.edit();
            editor.remove(key);
            editor.commit();
        }

        if (key.equals("Username"))
            _username = sharedPreferences.getString("Username", "");
        else if (key.equals("Realm"))
            _realm = sharedPreferences.getString("Realm", "");
        else if (key.equals("Local File Path"))
        {
            _localFilePath = sharedPreferences.getString("Local File Path", "");
            // TODO: clean this up?
            ArrayList<String> paths = null;
            if (! _localFilePath.equals(""))
            {
                paths = new ArrayList<String>(Arrays.asList(new String[]{_localFilePath}));
            }
            _xps.setLocalFilePaths(paths);
        }
        else if (key.equals("Period"))
        {
            _period =
                Long.valueOf(sharedPreferences.getString("Period", "5000"));
        }
        else if (key.equals("Iterations"))
        {
            _iterations =
                Integer.valueOf(sharedPreferences.getString("Iterations", "1"));
        }
        else if (key.equals("Desired XPS Accuracy"))
        {
            _desiredXpsAccuracy = Integer.valueOf(sharedPreferences.getString("Desired XPS Accuracy", "30"));
        }
        else if (key.equals("Tiling Path"))
        {
            _tilingPath = sharedPreferences.getString("Tiling Path", "");
        }
        else if (key.equals("Max Data Per Session"))
        {
            _maxDataSizePerSession =
                Long.valueOf(sharedPreferences.getString("Max Data Per Session", "0"));
        }
        else if (key.equals("Max Data Total"))
        {
            _maxDataSizeTotal =
                Long.valueOf(sharedPreferences.getString("Max Data Total", "0"));
        }
        else if (key.equals("Street Address Lookup"))
        {
            String setting = sharedPreferences.getString("Street Address Lookup", "None");
            if (setting.equals("None"))
            {
                _streetAddressLookup = WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP;
            }
            else if (setting.equals("Limited"))
            {
                _streetAddressLookup = WPSStreetAddressLookup.WPS_LIMITED_STREET_ADDRESS_LOOKUP;
            }
            else if (setting.equals("Full"))
            {
                _streetAddressLookup = WPSStreetAddressLookup.WPS_FULL_STREET_ADDRESS_LOOKUP;
            }
        }
        _xps.setTiling(_tilingPath,
                       _maxDataSizePerSession,
                       _maxDataSizeTotal,
                       null);
    }

    private String _username = null, _realm = null;
    private String _localFilePath = null;
    private long _period;
    private int _iterations;
    private int _desiredXpsAccuracy;
    private String _tilingPath = null;
    private long _maxDataSizePerSession;
    private long _maxDataSizeTotal;
    private WPSStreetAddressLookup _streetAddressLookup;
    private XPS _xps;
    private TextView _tv = null;
    private Handler _handler;
}
