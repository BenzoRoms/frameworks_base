/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.os.UserHandle;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.internal.util.benzo.WeatherController;
import com.android.internal.util.benzo.WeatherControllerImpl;
import com.android.internal.widget.LockPatternUtils;

import java.util.Date;
import java.text.NumberFormat;
import java.util.Locale;

public class KeyguardStatusView extends GridLayout implements
        WeatherController.Callback  {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final String TAG = "KeyguardStatusView";

    private final LockPatternUtils mLockPatternUtils;
    private final AlarmManager mAlarmManager;

    private TextView mAlarmStatusView;
    private TextClock mDateView;
    private TextClock mClockView;
    private TextView mAmbientDisplayBatteryView;
    private TextView mOwnerInfo;
    private View mWeatherView;
    private TextView mWeatherCity;
    private TextView mWeatherWind;
    private ImageView mWeatherConditionImage;
    private Drawable mWeatherConditionDrawable;
    private TextView mWeatherCurrentTemp;
    private TextView mWeatherHumidity;
    private TextView mWeatherConditionText;
    private TextView mWeatherTimestamp;

    private boolean mShowWeather;
    private int mIconNameValue = 0;

    private WeatherController mWeatherController;

    //On the first boot, keyguard will start to receiver TIME_TICK intent.
    //And onScreenTurnedOff will not get called if power off when keyguard is not started.
    //Set initial value to false to skip the above case.
    private boolean enableRefresh = false;

    private final int mWarningColor = 0xfff4511e; // deep orange 600
    private int mIconColor;
    private int mPrimaryTextColor;

    private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onTimeChanged() {
            if (enableRefresh) {
                refresh();
            }
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            if (showing) {
                if (DEBUG) Slog.v(TAG, "refresh statusview showing:" + showing);
                refresh();
                updateOwnerInfo();
            }
        }

        @Override
        public void onStartedWakingUp() {
            setEnableMarquee(true);
            enableRefresh = true;
            refresh();
        }

        @Override
        public void onFinishedGoingToSleep(int why) {
            setEnableMarquee(false);
            enableRefresh = false;
        }

        @Override
        public void onUserSwitchComplete(int userId) {
            refresh();
            updateOwnerInfo();
        }
    };

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mLockPatternUtils = new LockPatternUtils(getContext());
        mWeatherController = new WeatherControllerImpl(mContext);
    }

    private void setEnableMarquee(boolean enabled) {
        if (DEBUG) Log.v(TAG, (enabled ? "Enable" : "Disable") + " transport text marquee");
        if (mAlarmStatusView != null) mAlarmStatusView.setSelected(enabled);
        if (mOwnerInfo != null) mOwnerInfo.setSelected(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAlarmStatusView = (TextView) findViewById(R.id.alarm_status);
        mDateView = (TextClock) findViewById(R.id.date_view);
        mClockView = (TextClock) findViewById(R.id.clock_view);
        mDateView.setShowCurrentUserTime(true);
        mClockView.setShowCurrentUserTime(true);
        mAmbientDisplayBatteryView = (TextView) findViewById(R.id.ambient_display_battery_view);
        mOwnerInfo = (TextView) findViewById(R.id.owner_info);
        mWeatherView = findViewById(R.id.keyguard_weather_view);
        mWeatherCity = (TextView) findViewById(R.id.city);
        mWeatherWind = (TextView) findViewById(R.id.wind);
        mWeatherConditionImage = (ImageView) findViewById(R.id.weather_image);
        mWeatherCurrentTemp = (TextView) findViewById(R.id.current_temp);
        mWeatherHumidity = (TextView) findViewById(R.id.humidity);
        mWeatherConditionText = (TextView) findViewById(R.id.condition);
        mWeatherTimestamp = (TextView) findViewById(R.id.timestamp);
        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(mContext).isDeviceInteractive();
        setEnableMarquee(shouldMarquee);
        refresh();
        updateOwnerInfo();

        // Disable elegant text height because our fancy colon makes the ymin value huge for no
        // reason.
        mClockView.setElegantTextHeight(false);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_big_font_size));
        mClockView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
    }

    private int getLockClockFont() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_CLOCK_FONTS, 0);
    }

    public void hideLockscreenItems() {
        if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HIDE_LOCKSCREEN_CLOCK, 1) == 1) {
            mClockView = (TextClock) findViewById(R.id.clock_view);
            mClockView.setVisibility(View.VISIBLE);
        } else {
            mClockView = (TextClock) findViewById(R.id.clock_view);
            mClockView.setVisibility(View.GONE);
        }
        if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HIDE_LOCKSCREEN_DATE, 1) == 1) {
            mDateView = (TextClock) findViewById(R.id.date_view);
            mDateView.setVisibility(View.VISIBLE);
        } else {
            mDateView = (TextClock) findViewById(R.id.date_view);
            mDateView.setVisibility(View.GONE);
        }
    }

    public void refreshTime() {
        mDateView.setFormat24Hour(Patterns.dateView);
        mDateView.setFormat12Hour(Patterns.dateView);

        mClockView.setFormat12Hour(Patterns.clockView12);
        mClockView.setFormat24Hour(Patterns.clockView24);
    }

    private void refresh() {
        AlarmManager.AlarmClockInfo nextAlarm =
                mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT);
        Patterns.update(mContext, nextAlarm != null);

        refreshTime();
        refreshAlarmStatus(nextAlarm);
        hideLockscreenItems();
        refreshLockFont();
        updateWeatherSettings(false);
    }

    void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            String alarm = formatNextAlarm(mContext, nextAlarm);
            mAlarmStatusView.setText(alarm);
            mAlarmStatusView.setContentDescription(
                    getResources().getString(R.string.keyguard_accessibility_next_alarm, alarm));
            mAlarmStatusView.setVisibility(View.VISIBLE);
        } else {
            mAlarmStatusView.setVisibility(View.GONE);
        }
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
        if (info == null) {
            return "";
        }
        String skeleton = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser())
                ? "EHm"
                : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    private void updateOwnerInfo() {
        if (mOwnerInfo == null) return;
        String ownerInfo = getOwnerInfo();
        if (!TextUtils.isEmpty(ownerInfo)) {
            mOwnerInfo.setVisibility(View.VISIBLE);
            mOwnerInfo.setText(ownerInfo);
        } else {
            mOwnerInfo.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
        hideLockscreenItems();
        updateWeatherSettings(false);
        mWeatherController.addCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
        mWeatherController.removeCallback(this);
    }

    private String getOwnerInfo() {
        String info = null;
        if (mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
            // Use the device owner information set by device policy client via
            // device policy manager.
            info = mLockPatternUtils.getDeviceOwnerInfo();
        } else {
            // Use the current user owner information if enabled.
            final boolean ownerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled(
                    KeyguardUpdateMonitor.getCurrentUser());
            if (ownerInfoEnabled) {
                info = mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
            }
        }
        return info;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private void refreshBatteryInfo() {
        final Resources res = getContext().getResources();
        KeyguardUpdateMonitor.BatteryStatus batteryStatus =
                KeyguardUpdateMonitor.getInstance(mContext).getBatteryStatus();

        mPrimaryTextColor =
                res.getColor(R.color.keyguard_default_primary_text_color);
        mIconColor =
                res.getColor(R.color.keyguard_default_primary_text_color);

        String percentage = "";
        int resId = 0;
        final int lowLevel = res.getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
        final boolean useWarningColor = batteryStatus == null || batteryStatus.status == 1
                || (batteryStatus.level <= lowLevel && !batteryStatus.isPluggedIn());

        if (batteryStatus != null) {
            percentage = NumberFormat.getPercentInstance().format((double) batteryStatus.level / 100.0);
        }
        if (batteryStatus == null || batteryStatus.status == 1) {
            resId = R.drawable.ic_battery_unknown;
        } else {
            if (batteryStatus.level >= 96) {
                resId = batteryStatus.isPluggedIn()
                        ? R.drawable.ic_battery_charging_full : R.drawable.ic_battery_full;
            } else if (batteryStatus.level >= 90) {
                resId = batteryStatus.isPluggedIn()
                        ? R.drawable.ic_battery_charging_90 : R.drawable.ic_battery_90;
            } else if (batteryStatus.level >= 80) {
                resId = batteryStatus.isPluggedIn()
                        ? R.drawable.ic_battery_charging_80 : R.drawable.ic_battery_80;
            } else if (batteryStatus.level >= 60) {
                resId = batteryStatus.isPluggedIn()
                        ? R.drawable.ic_battery_charging_60 : R.drawable.ic_battery_60;
            } else if (batteryStatus.level >= 50) {
                resId = batteryStatus.isPluggedIn()
                        ? R.drawable.ic_battery_charging_50 : R.drawable.ic_battery_50;
            } else if (batteryStatus.level >= 30) {
                resId = batteryStatus.isPluggedIn()
                        ? R.drawable.ic_battery_charging_30 : R.drawable.ic_battery_30;
            } else if (batteryStatus.level >= lowLevel) {
                resId = batteryStatus.isPluggedIn()
                        ? R.drawable.ic_battery_charging_20 : R.drawable.ic_battery_20;
            } else {
                resId = batteryStatus.isPluggedIn()
                        ? R.drawable.ic_battery_charging_20 : R.drawable.ic_battery_alert;
            }
        }
        Drawable icon = resId > 0 ? res.getDrawable(resId).mutate() : null;
        if (icon != null) {
            icon.setTintList(ColorStateList.valueOf(useWarningColor ? mWarningColor : mIconColor));

        mAmbientDisplayBatteryView.setText(percentage);
        mAmbientDisplayBatteryView.setTextColor(useWarningColor
                ? mWarningColor : mPrimaryTextColor);
        mAmbientDisplayBatteryView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
        }
    }

    private void refreshLockFont() {
        final Resources res = getContext().getResources();
        boolean isPrimary = UserHandle.getCallingUserId() == UserHandle.USER_OWNER;
        int lockClockFont = isPrimary ? getLockClockFont() : 0;

        if (lockClockFont == 0) {
            mClockView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        if (lockClockFont == 1) {
            mClockView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        if (lockClockFont == 2) {
            mClockView.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        }
        if (lockClockFont == 3) {
            mClockView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        }
        if (lockClockFont == 4) {
            mClockView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }
        if (lockClockFont == 5) {
            mClockView.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
        }
        if (lockClockFont == 6) {
            mClockView.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        }
        if (lockClockFont == 7) {
            mClockView.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
        }
        if (lockClockFont == 8) {
            mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        }
        if (lockClockFont == 9) {
            mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
        }
        if (lockClockFont == 10) {
            mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        }
        if (lockClockFont == 11) {
            mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        }
        if (lockClockFont == 12) {
            mClockView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        if (lockClockFont == 13) {
            mClockView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        }
    }

    public void setDozing(boolean dozing) {
        if (dozing && showBattery()) {
            refreshBatteryInfo();
            if (mAmbientDisplayBatteryView.getVisibility() != View.VISIBLE) {
                mAmbientDisplayBatteryView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mAmbientDisplayBatteryView.getVisibility() != View.GONE) {
                mAmbientDisplayBatteryView.setVisibility(View.GONE);
            }
        }
    }

    private boolean showBattery() {
        return Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.AMBIENT_DISPLAY_SHOW_BATTERY, 1) == 1;
    } 

    @Override
    public void onWeatherChanged(WeatherController.WeatherInfo info) {
        if (info.temp == null || info.condition == null) {
            mWeatherCity.setText(null);
            mWeatherWind.setText(null);
            mWeatherConditionDrawable = null;
            mWeatherCurrentTemp.setText(null);
            mWeatherHumidity.setText(null);
            mWeatherConditionText.setText(null);
            mWeatherTimestamp.setText(null);
            mWeatherView.setVisibility(View.GONE);
            updateWeatherSettings(true);
        } else {
            mWeatherCity.setText(info.city);
            mWeatherWind.setText(info.wind);
            mWeatherConditionDrawable = info.conditionDrawable;
            mWeatherCurrentTemp.setText(info.temp);
            mWeatherHumidity.setText(info.humidity);
            mWeatherConditionText.setText(info.condition);
            mWeatherTimestamp.setText(getCurrentDate());
            mWeatherView.setVisibility(mShowWeather ? View.VISIBLE : View.GONE);
            updateWeatherSettings(false);
        }
    }

    private String getCurrentDate() {
        Date now = new Date();
        long nowMillis = now.getTime();
        StringBuilder sb = new StringBuilder();
        sb.append(DateFormat.format("E", nowMillis));
        sb.append(" ");
        sb.append(DateFormat.getTimeFormat(getContext()).format(nowMillis));
        return sb.toString();
    }

    private void updateWeatherSettings(boolean forceHide) {
        final ContentResolver resolver = getContext().getContentResolver();
        final Resources res = getContext().getResources();
        View weatherPanel = findViewById(R.id.weather_panel);
        TextView noWeatherInfo = (TextView) findViewById(R.id.no_weather_info_text);
        mShowWeather = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_SHOW_WEATHER, 0) == 1;
        boolean showLocation = Settings.System.getInt(resolver,
                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION, 1) == 1;
        boolean showTimestamp = Settings.System.getInt(resolver,
                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_TIMESTAMP, 0) == 1;
        int iconNameValue = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, 0);
        int primaryTextColor =
                res.getColor(R.color.keyguard_default_primary_text_color);
        // primaryTextColor with a transparency of 70%
        int secondaryTextColor = (179 << 24) | (primaryTextColor & 0x00ffffff);
        // primaryTextColor with a transparency of 50%
        int alarmTextAndIconColor = (128 << 24) | (primaryTextColor & 0x00ffffff);
        int defaultIconColor =
                res.getColor(R.color.keyguard_default_icon_color);
        int maxAllowedNotifications = 6;
        int currentVisibleNotifications = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_VISIBLE_NOTIFICATIONS, 0);
        int hideMode = Settings.System.getInt(resolver,
                    Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL, 0);
        int numberOfNotificationsToHide = Settings.System.getInt(resolver,
                       Settings.System.LOCK_SCREEN_WEATHER_NUMBER_OF_NOTIFICATIONS, 4);
        boolean forceHideByNumberOfNotifications = false;

        if (hideMode == 0) {
            if (currentVisibleNotifications > maxAllowedNotifications) {
                forceHideByNumberOfNotifications = true;
            }
        } else if (hideMode == 1) {
            if (currentVisibleNotifications >= numberOfNotificationsToHide) {
                forceHideByNumberOfNotifications = true;
            }
        }

        if (mWeatherView != null) {
            mWeatherView.setVisibility(
                (mShowWeather && !forceHideByNumberOfNotifications) ? View.VISIBLE : View.GONE);
        }
        if (forceHide) {
            noWeatherInfo.setVisibility(View.VISIBLE);
            weatherPanel.setVisibility(View.GONE);
            mWeatherConditionText.setVisibility(View.GONE);
            mWeatherTimestamp.setVisibility(View.GONE);
        } else {
            noWeatherInfo.setVisibility(View.GONE);
            weatherPanel.setVisibility(View.VISIBLE);
            mWeatherConditionText.setVisibility(View.VISIBLE);
            mWeatherCity.setVisibility(showLocation ? View.VISIBLE : View.INVISIBLE);
            mWeatherTimestamp.setVisibility(showTimestamp ? View.VISIBLE : View.GONE);
        }

        mAlarmStatusView.setTextColor(alarmTextAndIconColor);
        mDateView.setTextColor(primaryTextColor);
        mClockView.setTextColor(primaryTextColor);
        noWeatherInfo.setTextColor(primaryTextColor);
        mWeatherCity.setTextColor(primaryTextColor);
        mWeatherConditionText.setTextColor(primaryTextColor);
        mWeatherCurrentTemp.setTextColor(primaryTextColor);
        mWeatherHumidity.setTextColor(secondaryTextColor);
        mWeatherWind.setTextColor(secondaryTextColor);
        mWeatherTimestamp.setTextColor(secondaryTextColor);

        if (mIconNameValue != iconNameValue) {
            mIconNameValue = iconNameValue;
            mWeatherController.updateWeather();
        }
        Drawable[] drawables = mAlarmStatusView.getCompoundDrawablesRelative();
        Drawable alarmIcon = null;
        mAlarmStatusView.setCompoundDrawablesRelative(null, null, null, null);
        if (drawables[0] != null) {
            alarmIcon = drawables[0];
            alarmIcon.setColorFilter(alarmTextAndIconColor, Mode.MULTIPLY);
        }
        mAlarmStatusView.setCompoundDrawablesRelative(alarmIcon, null, null, null);
        mWeatherConditionImage.setImageDrawable(null);
        Drawable weatherIcon = mWeatherConditionDrawable;
        mWeatherConditionImage.setImageDrawable(weatherIcon);
    }

    // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // This is an optimization to ensure we only recompute the patterns when the inputs change.
    private static final class Patterns {
        static String dateView;
        static String clockView12;
        static String clockView24;
        static String cacheKey;

        static void update(Context context, boolean hasAlarm) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            final String dateViewSkel = res.getString(hasAlarm
                    ? R.string.abbrev_wday_month_day_no_year_alarm
                    : R.string.abbrev_wday_month_day_no_year);
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);
            final String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (key.equals(cacheKey)) return;

            dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);

            clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
            // CLDR insists on adding an AM/PM indicator even though it wasn't in the skeleton
            // format.  The following code removes the AM/PM indicator if we didn't want it.
            if (!clockView12Skel.contains("a")) {
                clockView12 = clockView12.replaceAll("a", "").trim();
            }

            clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

            // Use fancy colon.
            clockView24 = clockView24.replace(':', '\uee01');
            clockView12 = clockView12.replace(':', '\uee01');

            cacheKey = key;
        }
    }
}
