package com.youview.tinydnssd;

import android.net.nsd.NsdServiceInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by gabriel on 3/16/17.
 */

public class Service {
    private String host;
    private int port;
    private String serviceName;
    private String fullName;
    private List<String> addresses;
    private Map<String, String> attributes;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getFullName() { return fullName; }

    public void setFullName(String fullName) { this.fullName = fullName; }

    public List<String> getAddresses() {
        if (addresses == null) {
            addresses = new ArrayList<>();
        }

        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public static Service convert(NsdServiceInfo serviceInfo) {
        Service service = new Service();

        if (serviceInfo != null) {
            if (service.getHost() != null) {
                service.setHost(serviceInfo.getHost().toString());
                service.setFullName(serviceInfo.getHost().getHostName() + serviceInfo.getServiceType());
                service.getAddresses().add(serviceInfo.getHost().getHostAddress());
            }

            service.setPort(serviceInfo.getPort());
            service.setServiceName(serviceInfo.getServiceName());
        }

        return service;
    }

    public static Service convert (MDNSDiscover.Result result) {
        Service service = new Service();

        if (result != null) {
            service.setHost(result.srv.target);
            service.setPort(result.srv.port);
            service.setServiceName(parseFQDN(result.srv.fqdn));
            service.setFullName(result.srv.fqdn);
            service.getAddresses().add(result.a.ipaddr);
            service.setAttributes(result.txt.dict);
        }

        return service;
    }

    private static String parseFQDN(String fqdn) {
        String pattern = "^[^.]*";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(fqdn);

        return m.find() ? m.group(0) : fqdn;
    }
}
