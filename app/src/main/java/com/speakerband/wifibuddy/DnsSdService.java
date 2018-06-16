package com.speakerband.wifibuddy;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * La clase para almacenar la información del servicio Bonjour que se anuncia a través de una conexión Wi-Fi P2P.
 */
public class DnsSdService{

    private String instanceName;
    private String registrationType;
    private WifiP2pDevice srcDevice;

    public DnsSdService(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
        this.instanceName = instanceName;
        this.registrationType = registrationType;
        this.srcDevice = srcDevice;
    }

    public String getInstanceName() { return instanceName; }
    public WifiP2pDevice getSrcDevice() {
        //if(srcDevice!=null)
        // TODO da problemas, usar con mucho cuidado porque sino regresa null
            return srcDevice;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DnsSdService)) {
            return false;
        } else {
            DnsSdService other = (DnsSdService) o;
            return other.srcDevice.deviceName.equals(this.srcDevice.deviceName)
                && other.instanceName.equals(this.instanceName)
                && other.srcDevice.deviceAddress.equals(this.srcDevice.deviceAddress);
        }
    }
}
