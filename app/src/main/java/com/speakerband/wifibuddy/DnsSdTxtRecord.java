package com.speakerband.wifibuddy;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Map;

/**
 * Una clase para almacenar un registro TXT de Bonjour.
 */
public class DnsSdTxtRecord {

    private String fullDomain;
    private Map<String, String> record;
    private WifiP2pDevice device;

    public DnsSdTxtRecord(String fullDomain, Map<String, String> record, WifiP2pDevice device) {
        this.fullDomain = fullDomain;
        this.record = record;
        this.device = device;
    }

    public Map getRecord() {
        return record;
    }
}
