/*
 * RapidPro Android Channel - Relay SMS messages where MNO connections aren't practical.
 * Copyright (C) 2014 Nyaruka, UNICEF
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.rapidpro.androidchannel;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;

public class BaseActivity extends FragmentActivity {

    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        GCMRegistrar.checkDevice(this);
        GCMRegistrar.checkManifest(this);

        String regId = GCMRegistrar.getRegistrationId(this);
        if (regId.equals("")) {
            GCMRegistrar.register(this, Config.GCM_APP_ID);
        } else {
            regId = GCMRegistrar.getRegistrationId(this);
            SettingsActivity.setGCM(this, regId);
            RapidPro.get().sync(true);
        }
    }

    public void onSync(View v){
        RapidPro.get().sync(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);

        // show the settings menu always in debug mode
        if (BuildConfig.DEBUG) {
            MenuItem menuItem = menu.findItem(R.id.menu_settings);
            if (menuItem != null) {
                menuItem.setVisible(true);
            }

            MenuItem menuItem = menu.findItem(R.id.action_debug);
            if (menuItem != null) {
                menuItem.setVisible(true);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_Settings) {
            startActivity(new Intent(Intents.SHOW_SETTINGS));
            return true;
        } else if (id == R.id.action_debug) {
            sendBugReport();
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendBugReport() {

        // Log our build and device details
        StringBuilder info = new StringBuilder();
        info.append("Version: " + BuildConfig.VERSION_NAME + "; " + BuildConfig.VERSION_CODE);
        info.append("\n  OS: " + System.getProperty("os.version") + " (API " + Build.VERSION.SDK_INT + ")");
        info.append("\n  Model: " + android.os.Build.MODEL + " (" + android.os.Build.DEVICE + ")");
        RapidPro.LOG.d(info.toString());

        // Generate a logcat file
        File outputFile = new File(Environment.getExternalStorageDirectory(), "android-channel-debug.txt");

        try {
            Runtime.getRuntime().exec("logcat -d -f " + outputFile.getAbsolutePath() + "  \"*:E RapidPro:*\" ");
        } catch (Throwable t) {
            RapidPro.LOG.e("Failed to generate report", t);
        }

        ShareCompat.IntentBuilder.from(this)
                .setType("message/rfc822")
                .addEmailTo("support@rapidpro.io")
                .setSubject("RapiPro Android Channel Bug Report")
                .setText("Please include specific details on the error you encountered.")
                .setStream(Uri.fromFile(outputFile))
                .setChooserTitle("Send Email")
                .startChooser();
    }
}
