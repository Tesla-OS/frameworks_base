/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.statusbar;

import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.util.Log;

public class ThemeAccentUtils {
    public static final String TAG = "ThemeAccentUtils";

    private static final String[] ACCENTS = {
        "default_accent", // 0
        "com.accents.red", // 1
        "com.accents.pink", // 2
        "com.accents.purple", // 3
        "com.accents.deeppurple", // 4
        "com.accents.indigo", // 5
        "com.accents.blue", // 6
        "com.accents.lightblue", // 7
        "com.accents.cyan", // 8
        "com.accents.teal", // 9
        "com.accents.green", // 10
        "com.accents.lightgreen", // 11
        "com.accents.lime", // 12
        "com.accents.yellow", // 13
        "com.accents.amber", // 14
        "com.accents.orange", // 15
        "com.accents.deeporange", // 16
        "com.accents.brown", // 17
        "com.accents.grey", // 18
        "com.accents.bluegrey", // 19
        "com.accents.black", // 20
        "com.accents.white", // 21
    };

    private static final String[] DARK_THEMES = {
        "com.android.system.theme.dark", // 0
        "com.android.settings.theme.dark", // 1
        "com.android.dui.theme.dark", // 2
        "com.android.gboard.theme.dark", // 3
    };

    private static final String[] BLACK_THEMES = {
        "com.android.system.theme.black", // 0
        "com.android.settings.theme.black", // 1
        "com.android.dui.theme.black", // 2
        "com.android.gboard.theme.black", // 3
    };

    private static final String STOCK_DARK_THEME = "com.android.systemui.theme.dark";

    // Switches theme accent from to another or back to stock
    public static void updateAccents(IOverlayManager om, int userId, int accentSetting) {
        if (accentSetting == 0) {
            unloadAccents(om, userId);
        } else if (accentSetting < 20) {
            try {
                om.setEnabled(ACCENTS[accentSetting],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change theme", e);
            }
        } else if (accentSetting == 20) {
            try {
                // If using a dark theme we use the white accent, otherwise use the black accent
                if (isUsingDarkerThemes(om, userId)) {
                    om.setEnabled(ACCENTS[21],
                            true, userId);
                } else {
                    om.setEnabled(ACCENTS[20],
                            true, userId);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change theme", e);
            }
        }
    }

    // Unload all the theme accents
    public static void unloadAccents(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 1; i < ACCENTS.length; i++) {
            String accent = ACCENTS[i];
            try {
                om.setEnabled(accent,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Check for the dark system theme - systemui_theme_style_dark
    public static boolean isUsingDarkTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(DARK_THEMES[0],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Check for the dark system theme - systemui_theme_style_black
    public static boolean isUsingBlackTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(BLACK_THEMES[0],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Check for the darker system themes
    // DARK_THEMES, BLACK_THEMES
    public static boolean isUsingDarkerThemes(IOverlayManager om, int userId) {

        if (isUsingDarkTheme(om, userId) == true) return true;

        if (isUsingBlackTheme(om, userId) == true) return true;

        return false;
    }

    // Check for theme disambiguity
    public static boolean disambiguateDarkTheme(IOverlayManager om, int userId,
            boolean useDarkTheme, int setTheme) {

        // Default
        if (setTheme == 0) {
            // could use dark theme based on wallpaper
            if ((useDarkTheme == true)) {
                // true, if not already using dark theme
                if (isUsingDarkTheme(om, userId) == true) return false;
                return true;
            } else {
                // true, if using any darker themes
                if (isUsingDarkerThemes(om, userId) == true) return true;
                return false;
            }
        }

        // Light
        if (setTheme == 1) {
            if ((useDarkTheme == true)) {
                // undefined. should never happen. Light with darktheme. Pfff.
                return false;
            } else {
                // true, if any darker themes are used
                if (isUsingDarkerThemes(om, userId) == true) return true;
                return false;
            }
        }

        // Darker themese follow now.
        if (useDarkTheme == false) return false;

        // Dark theme, true, if we don't use it currently
        if ((setTheme == 2) && (isUsingDarkTheme(om, userId) == false))
            return true;

        // Black theme, true, if we don't use it currently
        if ((setTheme == 3) && (isUsingBlackTheme(om, userId) == false))
            return true;

        return false;
    }

    public static void handleDarkTheme(IOverlayManager om, int userId, boolean useDarkTheme) {

        for (String theme : DARK_THEMES) {
            try {
                om.setEnabled(theme,
                        useDarkTheme, userId);
                unfuckBlackWhiteAccent(om, userId);
                if (useDarkTheme) {
                    unloadStockDarkTheme(om, userId);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change theme", e);
            }
        }
    }

    public static void handleBlackTheme(IOverlayManager om, int userId, boolean useDarkTheme) {

        for (String theme : BLACK_THEMES) {
            try {
                om.setEnabled(theme,
                        useDarkTheme, userId);
                unfuckBlackWhiteAccent(om, userId);
                if (useDarkTheme) {
                    unloadStockDarkTheme(om, userId);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change theme", e);
            }
        }
    }

    public static void setLightDarkTheme(IOverlayManager om, int userId, boolean useDarkTheme, int setDarkTheme) {

        if (useDarkTheme == false) { // we are reverting to light theme
            handleDarkTheme(om, userId, useDarkTheme);
            handleBlackTheme(om, userId, useDarkTheme);
            return;
        }

        // dark themes
        if ((setDarkTheme == 0) || (setDarkTheme == 2)) {
            handleBlackTheme(om, userId, false);
            handleDarkTheme(om, userId, useDarkTheme);
            return;
        }

        if (setDarkTheme == 3) {
            handleDarkTheme(om, userId, false);
            handleBlackTheme(om, userId, useDarkTheme);
            return;
        }

        return;
    }

    // Check for black and white accent overlays
    public static void unfuckBlackWhiteAccent(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            if (isUsingDarkerThemes(om, userId)) {
                themeInfo = om.getOverlayInfo(ACCENTS[20],
                        userId);
                if (themeInfo != null && themeInfo.isEnabled()) {
                    om.setEnabled(ACCENTS[20],
                            false /*disable*/, userId);
                    om.setEnabled(ACCENTS[21],
                            true, userId);
                }
            } else {
                themeInfo = om.getOverlayInfo(ACCENTS[21],
                        userId);
                if (themeInfo != null && themeInfo.isEnabled()) {
                    om.setEnabled(ACCENTS[21],
                            false /*disable*/, userId);
                    om.setEnabled(ACCENTS[20],
                            true, userId);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Unloads the stock dark theme
    public static void unloadStockDarkTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(STOCK_DARK_THEME,
                    userId);
            if (themeInfo != null && themeInfo.isEnabled()) {
                om.setEnabled(STOCK_DARK_THEME,
                        false /*disable*/, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Check for the white accent overlay
    public static boolean isUsingWhiteAccent(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(ACCENTS[21],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }
}
