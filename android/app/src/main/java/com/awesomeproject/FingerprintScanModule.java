package com.awesomeproject;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.awesomeproject.HAPI;
import com.awesomeproject.LAPI;

public class FingerprintScanModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext _reactContext;
    private HAPI _hapi;
    private LAPI _lapi;
    private int m_hDevice = 0;
    public FingerprintScanModule(@NonNull ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
        _hapi = new HAPI(_reactContext.getCurrentActivity(), null);
        _lapi = new LAPI(_reactContext.getCurrentActivity());
    }

    @NonNull
    @Override
    public String getName() {
        return "FingerprintScan";
    }

    @ReactMethod
    public void openDevice(Callback successCallback, Callback errorCallback) {
        String msg;
        m_hDevice = _lapi.OpenDeviceEx();
        if (m_hDevice==0) msg = "Can't open device !";
        else {
            msg = "OpenDevice() = OK";
        }
        _hapi.m_hDev = m_hDevice;

        successCallback.invoke(msg);
    }
}
