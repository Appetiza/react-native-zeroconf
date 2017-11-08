package com.balthazargronon.RCTZeroconf;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
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
import com.youview.tinydnssd.MDNSDiscover;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Created by Jeremy White on 8/1/2016.
 * Copyright © 2016 Balthazar Gronon MIT
 */
@TargetApi(16)
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

    public static final int RESOLVE_TIMEOUT = 5000;
    public static final int MAX_ATTEMPTS_TO_RESOLVE = 2;

    private final Map<NsdServiceInfo, Integer> resolutionAttempsByService = new HashMap<>();
    private boolean started;
    private boolean trasitioning;

    public ZeroconfModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNZeroconf";
    }

    @ReactMethod
    public void scan(String type, String protocol, String domain) {
        if (started) {
            return;
        }

        if (!trasitioning) {
            discoverServices(String.format("_%s._%s.", type, protocol), NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            trasitioning = true;
        }

        started = true;
    }

    @ReactMethod
    public void stop() {
        if (!started) {
            return;
        }

        if (!trasitioning) {
            stopServiceDiscovery(mDiscoveryListener);
            trasitioning = true;
        }

        started = false;
    }

    protected void resolve(NsdServiceInfo serviceInfo) {
        Integer attempts;
        String serviceName = serviceInfo.getServiceName() + "." + serviceInfo.getServiceType() + "local";

        synchronized (resolutionAttempsByService) {
            attempts = resolutionAttempsByService.get(serviceInfo);
            resolutionAttempsByService.put(serviceInfo, attempts + 1);
        }

        if ((attempts == null) || (attempts > MAX_ATTEMPTS_TO_RESOLVE)) {
            sendEvent(getReactApplicationContext(), EVENT_ERROR,
                    "Resolving service failed with message: Timeout " + serviceName );

            return;
        }

        WritableMap service = new WritableNativeMap();

        try {
            MDNSDiscover.Result serviceResult = MDNSDiscover.resolve(serviceName, RESOLVE_TIMEOUT);

            service.putString(KEY_SERVICE_NAME, getServiceName(serviceResult.srv.fqdn));
            service.putString(KEY_SERVICE_FULL_NAME, serviceResult.srv.fqdn);
            service.putString(KEY_SERVICE_HOST, serviceResult.srv.target);
            service.putInt(KEY_SERVICE_PORT, serviceResult.srv.port);

            WritableMap txt = new WritableNativeMap();
            for (Map.Entry<String, String> entry : serviceResult.txt.dict.entrySet()) {
                txt.putString(entry.getKey(), entry.getValue());
            }

            service.putMap(KEY_SERVICE_TXT, txt);

            WritableArray addresses = new WritableNativeArray();
            addresses.pushString(serviceResult.a.ipaddr);

            service.putArray(KEY_SERVICE_ADDRESSES, addresses);

            synchronized (ZeroconfModule.this) {
                if (started) {
                    sendEvent(getReactApplicationContext(), EVENT_RESOLVE, service);
                }
            }
        } catch (SocketTimeoutException timeoutException) {
            resolve(serviceInfo);
        } catch (IOException e) {
            String error = "Resolving service failed with message: " + e;
            sendEvent(getReactApplicationContext(), EVENT_ERROR, error);
        }
    }

    NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            String error = "Starting service discovery failed with code: " + errorCode;
            sendEvent(getReactApplicationContext(), EVENT_ERROR, error);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            String error = "Stopping service discovery failed with code: " + errorCode;
            sendEvent(getReactApplicationContext(), EVENT_ERROR, error);
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            synchronized (ZeroconfModule.this) {
                if (!started) {
                    stop();
                } else {
                    sendEvent(getReactApplicationContext(), EVENT_START, null);
                    trasitioning = false;
                }
            }
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            if (started) {
                discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, this);
            } else {
                sendEvent(getReactApplicationContext(), EVENT_STOP, null);
                trasitioning = false;
            }
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            synchronized (ZeroconfModule.this) {
                if (started) {
                    WritableMap service = new WritableNativeMap();
                    service.putString(KEY_SERVICE_NAME, serviceInfo.getServiceName());

                    sendEvent(getReactApplicationContext(), EVENT_FOUND, service);

                    synchronized (resolutionAttempsByService) {
                        resolutionAttempsByService.put(serviceInfo, 1);
                    }

                    resolve(serviceInfo);
                }
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            synchronized (ZeroconfModule.this) {
                if (started) {
                    WritableMap service = new WritableNativeMap();
                    service.putString(KEY_SERVICE_NAME, serviceInfo.getServiceName());
                    sendEvent(getReactApplicationContext(), EVENT_REMOVE, service);
                }
            }
        }
    };

    protected void discoverServices(String serviceType, int protocol, NsdManager.DiscoveryListener listener) {
        ((NsdManager) getReactApplicationContext().getSystemService(Context.NSD_SERVICE)).discoverServices(serviceType, protocol, listener);
    }

    protected void stopServiceDiscovery(NsdManager.DiscoveryListener listener) {
        ((NsdManager) getReactApplicationContext().getSystemService(Context.NSD_SERVICE)).stopServiceDiscovery(listener);
    }

    protected void sendEvent(ReactContext reactContext,
                             String eventName,
                             @Nullable Object params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private String getServiceName(String fqdn) {
        String pattern = "^[^.]*";
        Pattern r = Pattern.compile(pattern);

        Matcher m = r.matcher(fqdn);

        if (m.find()) {
            return m.group(0);
        }

        return fqdn;
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        stop();
    }
}
