/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings.System;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.nio.charset.Charset;

import com.android.settings.R;
import com.mediatek.wifi.Utf8ByteLengthFilter;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiApDialogExt;

import android.util.Log;
import java.util.UUID;

/**
 * Dialog to configure the SSID and security settings
 * for Access Point operation
 */
public class WifiApDialog extends AlertDialog implements View.OnClickListener,
        TextWatcher, AdapterView.OnItemSelectedListener {
    static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;

    private final DialogInterface.OnClickListener mListener;

    public static final int OPEN_INDEX = 0;
    public static final int WPA2_INDEX = 1;
    /// M: max length for ssid and password @{
    private static final int AP_SSID_MAX_LENGTH_BYTES = 32;
    private static final int AP_PSW_MAX_LENGTH_BYTES = 63;
    /// @}

    private View mView;
    private TextView mSsid;
    private int mSecurityTypeIndex = OPEN_INDEX;
    private EditText mPassword;
    private int mBandIndex = OPEN_INDEX;

    WifiConfiguration mWifiConfig;
    WifiManager mWifiManager;
    private Context mContext;
    /// M: @{
    private Spinner mSecurity;
    IWifiApDialogExt mExt;
    private Spinner mMaxConnSpinner;
    ///@}

    private static final String TAG = "WifiApDialog";

    public WifiApDialog(Context context, DialogInterface.OnClickListener listener,
            WifiConfiguration wifiConfig) {
        super(context);
        mListener = listener;
        mWifiConfig = wifiConfig;
        if (wifiConfig != null) {
            mSecurityTypeIndex = getSecurityTypeIndex(wifiConfig);
        }
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mContext =  context;
    }

    public static int getSecurityTypeIndex(WifiConfiguration wifiConfig) {
        if (wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
            return WPA2_INDEX;
        }
        return OPEN_INDEX;
    }

    public WifiConfiguration getConfig() {

        WifiConfiguration config = new WifiConfiguration();

        /**
         * TODO: SSID in WifiConfiguration for soft ap
         * is being stored as a raw string without quotes.
         * This is not the case on the client side. We need to
         * make things consistent and clean it up
         */
        config.SSID = mSsid.getText().toString();

        config.apBand = mBandIndex;

        switch (mSecurityTypeIndex) {
            case OPEN_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                return config;

            case WPA2_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                if (mPassword.length() != 0) {
                    String password = mPassword.getText().toString();
                    config.preSharedKey = password;
                }
                return config;
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = getContext();

        boolean mInit = true;
        mView = getLayoutInflater().inflate(R.layout.wifi_ap_dialog, null);
        mSecurity = ((Spinner) mView.findViewById(R.id.security));
        final Spinner mChannel = (Spinner) mView.findViewById(R.id.choose_channel);

        /// M: get plug in and set adapter for mSecurity @{
        mExt = UtilsExt.getWifiApDialogPlugin(context);
        mExt.setAdapter(context, mSecurity, R.array.wifi_ap_security);

        mMaxConnSpinner = ((Spinner) mView.findViewById(R.id.max_connection_num));
        mMaxConnSpinner.setOnItemSelectedListener(this);
        /// @}
        setView(mView);
        setInverseBackgroundForced(true);

        setTitle(R.string.wifi_tether_configure_ap_text);
        mView.findViewById(R.id.type).setVisibility(View.VISIBLE);
        mSsid = (TextView) mView.findViewById(R.id.ssid);
        mPassword = (EditText) mView.findViewById(R.id.password);

        ArrayAdapter <CharSequence> channelAdapter;
        String countryCode = mWifiManager.getCountryCode();
        if (!mWifiManager.isDualBandSupported() || countryCode == null) {
            //If no country code, 5GHz AP is forbidden
            Log.i(TAG,(!mWifiManager.isDualBandSupported() ? "Device do not support 5GHz " :"") 
                    + (countryCode == null ? " NO country code" :"") +  " forbid 5GHz");
            channelAdapter = ArrayAdapter.createFromResource(mContext,
                    R.array.wifi_ap_band_config_2G_only, android.R.layout.simple_spinner_item);
            mWifiConfig.apBand = 0;
        } else {
            channelAdapter = ArrayAdapter.createFromResource(mContext,
                    R.array.wifi_ap_band_config_full, android.R.layout.simple_spinner_item);
        }

        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        setButton(BUTTON_SUBMIT, context.getString(R.string.wifi_save), mListener);
        setButton(DialogInterface.BUTTON_NEGATIVE,
        context.getString(R.string.wifi_cancel), mListener);

        if (mWifiConfig != null) {
            mSsid.setText(mWifiConfig.SSID);
            if (mWifiConfig.apBand == 0) {
               mBandIndex = 0;
            } else {
               mBandIndex = 1;
            }

            /// M: set selection
            mSecurity.setSelection(mSecurityTypeIndex);
            if (mSecurityTypeIndex == WPA2_INDEX) {
                mPassword.setText(mWifiConfig.preSharedKey);
            }
        }

        mChannel.setAdapter(channelAdapter);
        mChannel.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    boolean mInit = true;
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                                               long id) {
                        if (!mInit) {
                            mBandIndex = position;
                            mWifiConfig.apBand = mBandIndex;
                            Log.i(TAG, "config on channelIndex : " + mBandIndex + " Band: " +
                                    mWifiConfig.apBand);
                        } else {
                            mInit = false;
                            mChannel.setSelection(mBandIndex);
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                }
        );

        /// M: init reset_oob Button and maxConnSpinner @{
        //SSID max length must shorter than 32 bytes
        mSsid.setFilters(new InputFilter[] {
                    new Utf8ByteLengthFilter(AP_SSID_MAX_LENGTH_BYTES)});
        mPassword.setFilters(new InputFilter[]  {
                    new Utf8ByteLengthFilter(AP_PSW_MAX_LENGTH_BYTES)});

        ((Button) mView.findViewById(R.id.reset_oob)).setOnClickListener(this);

        int maxConnValue = System.getInt(mContext.getContentResolver(),
            System.WIFI_HOTSPOT_MAX_CLIENT_NUM,
            System.WIFI_HOTSPOT_DEFAULT_CLIENT_NUM);
        mMaxConnSpinner.setSelection(maxConnValue - 1);
        /// @}
        mSsid.addTextChangedListener(this);
        mPassword.addTextChangedListener(this);
        ((CheckBox) mView.findViewById(R.id.show_password)).setOnClickListener(this);
        mSecurity.setOnItemSelectedListener(this);

        super.onCreate(savedInstanceState);

        showSecurityFields();
        validate();
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                (((CheckBox) mView.findViewById(R.id.show_password)).isChecked() ?
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    private void validate() {
        String mSsidString = mSsid.getText().toString();
        //M:DELSLMY-809 guoshuai 20160311(start)
        //if ((mSsid != null && mSsid.length() == 0)
        if ((mSsid != null && mSsid.getText().toString().trim().length() == 0)
        //M:DELSLMY-809 guoshuai 20160311(end)
                || ((mSecurityTypeIndex == WPA2_INDEX) && mPassword.length() < 8)
                || (mSsid != null &&
                Charset.forName("UTF-8").encode(mSsidString).limit() > 32)) {
            getButton(BUTTON_SUBMIT).setEnabled(false);
        } else {
            getButton(BUTTON_SUBMIT).setEnabled(true);
        }
    }

    public void onClick(View view) {
        if (view.getId() == R.id.show_password) {
            mPassword.setInputType(
                    InputType.TYPE_CLASS_TEXT | (((CheckBox) view).isChecked() ?
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                    InputType.TYPE_TEXT_VARIATION_PASSWORD));
        } else if (view.getId() == R.id.reset_oob) {
            String s = com.mediatek.custom.CustomProperties.getString(
                com.mediatek.custom.CustomProperties.MODULE_WLAN,
                com.mediatek.custom.CustomProperties.SSID,
                mContext.getString(
                com.android.internal.R.string.wifi_tether_configure_ssid_default));
            mSsid.setText(s);
            mSecurityTypeIndex = WPA2_INDEX;
            mSecurity.setSelection(mSecurityTypeIndex);

            String randomUUID = UUID.randomUUID().toString();
            //first 12 chars from xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
            String randomPassword = randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
            mPassword.setText(randomPassword);
        }
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void afterTextChanged(Editable editable) {
        validate();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(mSecurity)) {
            ///M: set index after item is selected @{
            mSecurityTypeIndex = position;
            Log.d(TAG, "mSecurityTypeIndex: " + mSecurityTypeIndex);
            /// @}
            showSecurityFields();
            validate();
        } else if (parent.equals(mMaxConnSpinner)) {
            int maxConnValue = position + 1;
            System.putInt(mContext.getContentResolver(),
                System.WIFI_HOTSPOT_MAX_CLIENT_NUM, maxConnValue);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void showSecurityFields() {
        if (mSecurityTypeIndex == OPEN_INDEX) {
            mView.findViewById(R.id.fields).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.fields).setVisibility(View.VISIBLE);
    }
    /**
     * M: close spinner dialog method
     */
    public void closeSpinnerDialog() {
        if (mSecurity != null && mSecurity.isPopupShowing()) {
            mSecurity.dismissPopup();
        }
    }
}