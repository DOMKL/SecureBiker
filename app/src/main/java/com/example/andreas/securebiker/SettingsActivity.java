package com.example.andreas.securebiker;


import android.annotation.TargetApi;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.example.andreas.securebiker.Fragments.AllPreferencesFragment;
import com.example.andreas.securebiker.Fragments.SeekBarPreference;
import com.example.andreas.securebiker.Fragments.SliderFragment;
import com.example.andreas.securebiker.Listener.PreferenceChangeListener;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    SharedPreferences pref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActionBar();
        addPreferencesFromResource(R.xml.pref_all);
        //FragmentTransaction tFrag = getFragmentManager().beginTransaction();
        //Add Layout for all Preferences
        //tFrag.add(android.R.id.content, new AllPreferencesFragment());
        //tFrag.commit();

        initializeSummarys();

        //get Preferences
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        //set listener to the Preferences
        listener = new PreferenceChangeListener(this);
        pref.registerOnSharedPreferenceChangeListener(listener);
    }

    private void initializeSummarys() {
        //Summary of Fences-Radius
        Preference radiusPref = this.findPreference(AllPreferencesFragment.KEY_FENCES_RADIUS);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int radius = sharedPreferences.getInt(AllPreferencesFragment.KEY_FENCES_RADIUS, 50) + 50;
        radiusPref.setSummary(this.getString(R.string.settings_summary).replace("$1", "" + radius));
        //Summary of Alarm-Timer
        Preference alarmTimerPref = this.findPreference(AllPreferencesFragment.KEY_ALARMDIALOGTIMER);
        alarmTimerPref.setSummary(sharedPreferences.getString(AllPreferencesFragment.KEY_ALARMDIALOGTIMER, "10"));
        /**Sumary of Alarmsound (Gibt den Pfad aus)
        Preference notiRingPref = this.findPreference(AllPreferencesFragment.KEY_NOTIFI_MESSAGE_RING);
        notiRingPref.setSummary(sharedPreferences.getString(AllPreferencesFragment.KEY_NOTIFI_MESSAGE_RING, ""));
       **/
        //Sumary of Vibration (On/Off)
        Preference notiVibPref = this.findPreference(AllPreferencesFragment.KEY_NOTIFI_MESSAGE_VIB);
        if (sharedPreferences.getBoolean(AllPreferencesFragment.KEY_NOTIFI_MESSAGE_VIB, false)) {
            notiVibPref.setSummary(R.string.pref_vibrate_on);
        } else {
            notiVibPref.setSummary(R.string.pref_vibrate_off);
        }

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = null;//getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * @Override public void onBuildHeaders(List<Header> target) {
     * super.onBuildHeaders(target);
     * loadHeadersFromResource(R.xml.pref_headers, target);
     * }
     **/

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // setupSimplePreferencesScreen();
    }


    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     * <p/>
     * private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
     *
     * @Override public boolean onPreferenceChange(Preference preference, Object value) {
     * String stringValue = value.toString();
     * <p/>
     * if (preference instanceof ListPreference) {
     * // For list preferences, look up the correct display value in
     * // the preference's 'entries' list.
     * ListPreference listPreference = (ListPreference) preference;
     * int index = listPreference.findIndexOfValue(stringValue);
     * <p/>
     * // Set the summary to reflect the new value.
     * preference.setSummary(
     * index >= 0
     * ? listPreference.getEntries()[index]
     * : null);
     * <p/>
     * } else if (preference instanceof RingtonePreference) {
     * // For ringtone preferences, look up the correct display value
     * // using RingtoneManager.
     * if (TextUtils.isEmpty(stringValue)) {
     * // Empty values correspond to 'silent' (no ringtone).
     * preference.setSummary(R.string.pref_ringtone_silent);
     * <p/>
     * } else {
     * Ringtone ringtone = RingtoneManager.getRingtone(
     * preference.getContext(), Uri.parse(stringValue));
     * <p/>
     * if (ringtone == null) {
     * // Clear the summary if there was a lookup error.
     * preference.setSummary(null);
     * } else {
     * // Set the summary to reflect the new ringtone display
     * // name.
     * String name = ringtone.getTitle(preference.getContext());
     * preference.setSummary(name);
     * }
     * }
     * <p/>
     * } else {
     * // For all other preferences, set the summary to the value's
     * // simple string representation.
     * preference.setSummary(stringValue);
     * }
     * return true;
     * }
     * };
     * <p/>
     * /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     * @see #//sBindPreferenceSummaryToValueListener
     * <p/>
     * private static void bindPreferenceSummaryToValue(Preference preference) {
     * // Set the listener to watch for value changes.
     * preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
     * <p/>
     * // Trigger the listener immediately with the preference's
     * // current value.
     * sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
     * PreferenceManager
     * .getDefaultSharedPreferences(preference.getContext())
     * .getString(preference.getKey(), ""));
     * }
     **/
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
        initializeSummarys();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName)
                || AllPreferencesFragment.class.getName().equals(fragmentName)
                || SliderFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            // bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            //bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

}
