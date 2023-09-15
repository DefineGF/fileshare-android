package com.cjm.fileshare.bean;

import androidx.annotation.NonNull;

import com.cjm.fileshare.config.InetConfig;

public class InetInfo {
    private int    inetType;
    private String ipSelf;
    private String ipWifi;

    public InetInfo() {
        inetType = InetConfig.NET_TYPE_NULL;
        ipSelf   = null;
        ipWifi   = null;
    }

    public int getInetType() {
        return inetType;
    }

    public void setInetType(int inetType) {
        this.inetType = inetType;
    }

    public String getIpSelf() {
        return ipSelf;
    }

    public void setIpSelf(String ipSelf) {
        this.ipSelf = ipSelf;
    }

    public String getIpWifi() {
        return ipWifi;
    }

    public void setIpWifi(String ipWifi) {
        this.ipWifi = ipWifi;
    }

    @NonNull
    @Override
    public String toString() {
        return "\tInetInfo: " +
                "\n\t\t网络类型 : " + (inetType ==  InetConfig.NET_TYPE_MOBILE ? "移动网络" : "移动热点") +
                "\n\t\t本机ip ：" + ipSelf +
                "\n\t\t热点ip : " + ipWifi;
    }
}
