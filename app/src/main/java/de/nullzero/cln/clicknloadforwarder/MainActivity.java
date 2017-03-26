package de.nullzero.cln.clicknloadforwarder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getSimpleName();

    @Bind(R.id.target_url)
    EditText viewTargetUrl;

    @Bind(R.id.target_auth_username)
    EditText viewTargetAuthUsername;

    @Bind(R.id.target_auth_password)
    EditText viewTargetAuthPassword;

    @Bind(R.id.target_local_url)
    EditText viewTargetHomeUrl;

    @Bind(R.id.wlan_ssid)
    EditText viewHomeWlanSSID;

    @Bind(R.id.save)
    Button viewSaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        loadSettings();

        viewSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
                finish();
            }
        });
    }

    private void loadSettings() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        viewTargetHomeUrl.setText(prefs.getString(ForwarderService.targetHomeURL, ""));
        viewTargetUrl.setText(prefs.getString(ForwarderService.targetURL, ""));
        viewTargetAuthUsername.setText(prefs.getString(ForwarderService.httpBasicAuthUsername, ""));
        viewTargetAuthPassword.setText(prefs.getString(ForwarderService.httpBasicAuthPassword, ""));
        viewHomeWlanSSID.setText(prefs.getString(ForwarderService.homeSSID, ""));
    }

    private void saveSettings() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        final SharedPreferences.Editor editor = prefs.edit();

        editor.putString(ForwarderService.targetHomeURL, viewTargetHomeUrl.getText().toString());
        editor.putString(ForwarderService.targetURL, viewTargetUrl.getText().toString());
        editor.putString(ForwarderService.httpBasicAuthUsername, viewTargetAuthUsername.getText().toString());
        editor.putString(ForwarderService.httpBasicAuthPassword, viewTargetAuthPassword.getText().toString());
        editor.putString(ForwarderService.homeSSID, viewHomeWlanSSID.getText().toString());

        editor.apply();
    }


    @Override
    protected void onResume() {
        super.onResume();
        startForwarderService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    private void startForwarderService() {
        Intent forwarderServiceIntent = new Intent(this, ForwarderServiceImpl.class);
        startService(forwarderServiceIntent);
    }
}
