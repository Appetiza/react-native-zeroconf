package com.balthazargronon.RCTZeroconf;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.youview.tinydnssd.DiscoverResolver;
import com.youview.tinydnssd.MDNSDiscover;
import com.youview.tinydnssd.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Created by Jeremy White on 8/1/2016.
 * Copyright © 2016 Balthazar Gronon MIT
 */
public class ZeroconfModule extends ReactContextBaseJavaModule {

    public static final String EVENT_START = "RNZeroconfStart";
    public static final String EVENT_STOP = "RNZeroconfStop";
    public static final String EVENT_ERROR = "RNZeroconfError";
    public static final String EVENT_FOUND = "RNZeroconfFound";
    public static final String EVENT_REMOVE = "RNZeroconfRemove";
    public static final String EVENT_RESOLVE = "RNZeroconfResolved";

    public static final String KEY_SERVICE_NAME = "name";
    public static final String KEY_SERVICE_FULL_NAME = "fullName";
    public static final String KEY_SERVICE_HOST = "host";
    public static final String KEY_SERVICE_PORT = "port";
    public static final String KEY_SERVICE_ADDRESSES = "addresses";
    public static final String KEY_SERVICE_TXT = "txt";

    protected DiscoverResolver.Listener mListener;
    protected DiscoverResolver mDiscoverResolver;

    public ZeroconfModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNZeroconf";
    }

    @ReactMethod
    public void scan(String type, String protocol, String domain) {
        String serviceType = String.format("_%s._%s.", type, protocol);

        mListener = new DiscoverResolver.Listener() {
            @Override
            public void onStartDiscoveryFailed(int errorCode) {
                String error = "Starting service discovery failed with code: " + errorCode;
                sendEvent(getReactApplicationContext(), EVENT_ERROR, error);
            }

            @Override
            public void onStopDiscoveryFailed(int errorCode) {
                String error = "Stopping service discovery failed with code: " + errorCode;
                sendEvent(getReactApplicationContext(), EVENT_ERROR, error);
            }

            @Override
            public void onDiscoveryStarted() {
                sendEvent(getReactApplicationContext(), EVENT_START, null);
            }

            @Override
            public void onDiscoveryStopped() {
                sendEvent(getReactApplicationContext(), EVENT_STOP, null);
            }

            @Override
            public void onServiceFound(Service service) {
                WritableMap serviceMap = new WritableNativeMap();
                serviceMap.putString(KEY_SERVICE_NAME, service.getServiceName());

                sendEvent(getReactApplicationContext(), EVENT_FOUND, serviceMap);
            }

            @Override
            public void onServiceLost(Service service) {
                WritableMap serviceMap = new WritableNativeMap();
                serviceMap.putString(KEY_SERVICE_NAME, service.getServiceName());
                sendEvent(getReactApplicationContext(), EVENT_REMOVE, serviceMap);
            }

            @Override
            public void onServiceResolved(Service service) {
                WritableMap serviceMap = new WritableNativeMap();
                serviceMap.putString(KEY_SERVICE_NAME, service.getServiceName());
                serviceMap.putString(KEY_SERVICE_FULL_NAME, service.getFullName());
                serviceMap.putString(KEY_SERVICE_HOST, service.getHost());
                serviceMap.putInt(KEY_SERVICE_PORT, service.getPort());

                WritableMap txt = new WritableNativeMap();
                for (Map.Entry<String, String> attribute : service.getAttributes().entrySet()) {
                    txt.putString(attribute.getKey(), attribute.getValue());
                }

                serviceMap.putMap(KEY_SERVICE_TXT, txt);

                WritableArray addresses = new WritableNativeArray();
                for (String address : service.getAddresses()) addresses.pushString(address);

                serviceMap.putArray(KEY_SERVICE_ADDRESSES, addresses);

                sendEvent(getReactApplicationContext(), EVENT_RESOLVE, serviceMap);
            }

            @Override
            public void onResolveFailed(int errorCode) {
                String error = "Resolving service failed with code: " + errorCode;
                sendEvent(getReactApplicationContext(), EVENT_ERROR, error);
            }
        };

        if (mDiscoverResolver == null) {
            mDiscoverResolver = new DiscoverResolver(getReactApplicationContext(), serviceType, mListener, 1000);
        }

        mDiscoverResolver.start();
    }

    @ReactMethod
    public void stop() {
        if (mDiscoverResolver != null) {
            mDiscoverResolver.stop();
        }

        mDiscoverResolver = null;
    }

    protected void sendEvent(ReactContext reactContext,
                             String eventName,
                             @Nullable Object params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        stop();
    }
}
