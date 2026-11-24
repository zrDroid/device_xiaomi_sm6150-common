/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.view.Display;
import android.view.WindowManager;
import androidx.preference.PreferenceManager;
import org.lineageos.settings.utils.FileUtils;

public final class ThermalUtils {
    private static final String THERMAL_CONTROL = "thermal_control";

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_BENCHMARK = 1;
    protected static final int STATE_BROWSER = 2;
    protected static final int STATE_CAMERA = 3;
    protected static final int STATE_DIALER = 4;
    protected static final int STATE_GAMING = 5;
    protected static final int STATE_NAVIGATION = 6;
    protected static final int STATE_STREAMING = 7;
    protected static final int STATE_VIDEO = 8;

    private static final String THERMAL_STATE_DEFAULT = "0";
    private static final String THERMAL_STATE_BENCHMARK = "10";
    private static final String THERMAL_STATE_BROWSER = "11";
    private static final String THERMAL_STATE_CAMERA = "12";
    private static final String THERMAL_STATE_DIALER = "8";
    private static final String THERMAL_STATE_GAMING = "13";
    private static final String THERMAL_STATE_NAVIGATION = "19";
    private static final String THERMAL_STATE_STREAMING = "14";
    private static final String THERMAL_STATE_VIDEO = "21";

    private static final String THERMAL_BENCHMARK = "thermal.benchmark=";
    private static final String THERMAL_BROWSER = "thermal.browser=";
    private static final String THERMAL_CAMERA = "thermal.camera=";
    private static final String THERMAL_DIALER = "thermal.dialer=";
    private static final String THERMAL_GAMING = "thermal.gaming=";
    private static final String THERMAL_NAVIGATION = "thermal.navigation=";
    private static final String THERMAL_STREAMING = "thermal.streaming=";
    private static final String THERMAL_VIDEO = "thermal.video=";

    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";

    private Display mDisplay;
    private SharedPreferences mSharedPrefs;

    protected ThermalUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        WindowManager mWindowManager = context.getSystemService(WindowManager.class);
        mDisplay = mWindowManager.getDefaultDisplay();
    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, ThermalService.class), UserHandle.CURRENT);
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(THERMAL_CONTROL, profiles).apply();
    }

    private String getValue() {
        String value = mSharedPrefs.getString(THERMAL_CONTROL, null);

        if (value == null || value.isEmpty()) {
            value = THERMAL_BENCHMARK + ":" + THERMAL_BROWSER + ":" + THERMAL_CAMERA + ":"
                    + THERMAL_DIALER + ":" + THERMAL_GAMING + ":" + THERMAL_NAVIGATION + ":"
                    + THERMAL_STREAMING + ":" + THERMAL_VIDEO;
            writeValue(value);
        }
        return value;
    }

    private static String[] getPackages(String mode) {
        return mode.substring(mode.indexOf('=') + 1).split(",");
    }

    private static boolean containsPackage(String mode, String packageName) {
        for (String savedPackage : getPackages(mode)) {
            if (!savedPackage.isEmpty() && packageName.equals(savedPackage)) {
                return true;
            }
        }
        return false;
    }

    protected void writePackage(String packageName, int mode) {
        String[] modes = getValue().split(":");
        for (int i = 0; i < modes.length; i++) {
            StringBuilder updatedMode =
                    new StringBuilder(modes[i].substring(0, modes[i].indexOf('=') + 1));
            for (String savedPackage : getPackages(modes[i])) {
                if (!savedPackage.isEmpty() && !packageName.equals(savedPackage)) {
                    updatedMode.append(savedPackage).append(',');
                }
            }
            modes[i] = updatedMode.toString();
        }
        String finalString;

        switch (mode) {
            case STATE_BENCHMARK:
                modes[0] = modes[0] + packageName + ",";
                break;
            case STATE_BROWSER:
                modes[1] = modes[1] + packageName + ",";
                break;
            case STATE_CAMERA:
                modes[2] = modes[2] + packageName + ",";
                break;
            case STATE_DIALER:
                modes[3] = modes[3] + packageName + ",";
                break;
            case STATE_GAMING:
                modes[4] = modes[4] + packageName + ",";
                break;
            case STATE_NAVIGATION:
                modes[5] = modes[5] + packageName + ",";
                break;
            case STATE_STREAMING:
                modes[6] = modes[6] + packageName + ",";
                break;
            case STATE_VIDEO:
                modes[7] = modes[7] + packageName + ",";
                break;
        }

        finalString = modes[0] + ":" + modes[1] + ":" + modes[2] + ":" + modes[3] + ":" + modes[4]
                + ":" + modes[5] + ":" + modes[6] + ":" + modes[7];

        writeValue(finalString);
    }

    protected int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        int state = STATE_DEFAULT;

        if (containsPackage(modes[0], packageName)) {
            state = STATE_BENCHMARK;
        } else if (containsPackage(modes[1], packageName)) {
            state = STATE_BROWSER;
        } else if (containsPackage(modes[2], packageName)) {
            state = STATE_CAMERA;
        } else if (containsPackage(modes[3], packageName)) {
            state = STATE_DIALER;
        } else if (containsPackage(modes[4], packageName)) {
            state = STATE_GAMING;
        } else if (containsPackage(modes[5], packageName)) {
            state = STATE_NAVIGATION;
        } else if (containsPackage(modes[6], packageName)) {
            state = STATE_STREAMING;
        } else if (containsPackage(modes[7], packageName)) {
            state = STATE_VIDEO;
        }

        return state;
    }

    protected void setDefaultThermalProfile() {
        FileUtils.writeLine(THERMAL_SCONFIG, THERMAL_STATE_DEFAULT);
    }

    protected void setThermalProfile(String packageName) {
        String value = getValue();
        String modes[];
        String state = THERMAL_STATE_DEFAULT;

        if (value != null) {
            modes = value.split(":");

            if (containsPackage(modes[0], packageName)) {
                state = THERMAL_STATE_BENCHMARK;
            } else if (containsPackage(modes[1], packageName)) {
                state = THERMAL_STATE_BROWSER;
            } else if (containsPackage(modes[2], packageName)) {
                state = THERMAL_STATE_CAMERA;
            } else if (containsPackage(modes[3], packageName)) {
                state = THERMAL_STATE_DIALER;
            } else if (containsPackage(modes[4], packageName)) {
                state = THERMAL_STATE_GAMING;
            } else if (containsPackage(modes[5], packageName)) {
                state = THERMAL_STATE_NAVIGATION;
            } else if (containsPackage(modes[6], packageName)) {
                state = THERMAL_STATE_STREAMING;
            } else if (containsPackage(modes[7], packageName)) {
                state = THERMAL_STATE_VIDEO;
            }
        }

        FileUtils.writeLine(THERMAL_SCONFIG, state);
    }
}
