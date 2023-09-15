package com.cjm.fileshare.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.cjm.fileshare.config.InetConfig;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class InternetUtil {
    private static final String TAG = "InternetUtil";

    public static int getNetType(Context context) {
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                Log.i(TAG, "当前网络类型: " + InetConfig.NET_TYPE_MOBILE);
                return InetConfig.NET_TYPE_MOBILE;
            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.i(TAG, "当前网络类型: " + InetConfig.NET_TYPE_WIFI);
                return InetConfig.NET_TYPE_WIFI;
            }
        }
        return InetConfig.NET_TYPE_NULL;
    }

    private static WifiManager getWifiManager(Context context) {
//        if (!wifiManager.isWifiEnabled()) {    // 判断 wifi 是否可用
//            wifiManager.setWifiEnabled(true);
//        }
        return (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static String getLocalIp(Context context) {
        int net_type = getNetType(context);
        if (net_type == InetConfig.NET_TYPE_MOBILE) {
            return getLocalIpByMobile();
        } else if (net_type == InetConfig.NET_TYPE_WIFI) {
            return getLocalIpByWifi(context);
        }
        return null;
    }


    public static String getWifiIp(Context context) {
        WifiManager wifiManager = getWifiManager(context);
        DhcpInfo dhcpinfo = wifiManager.getDhcpInfo();
        return intToIp(dhcpinfo.serverAddress);
    }


    /**
     * 通过 wifi 获取本地 ip 地址
     * @param context context
     * @return 当未连接 wifi时，返回 0.0.0.0
     */
    private static String getLocalIpByWifi(Context context) {
        WifiManager wifiManager = getWifiManager(context);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return intToIp(wifiInfo.getIpAddress());
    }

    private static String getLocalIpByMobile() {
        StringBuilder sb = new StringBuilder();
        String target    = null;
        try {
            Enumeration<NetworkInterface> mEnumeration = NetworkInterface.getNetworkInterfaces();
            for ( ;mEnumeration.hasMoreElements(); ) {
                NetworkInterface intf = mEnumeration.nextElement();
                for (Enumeration<InetAddress> enumIPAddr = intf.getInetAddresses(); enumIPAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIPAddr.nextElement();
                    String hostAddress = inetAddress.getHostAddress();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        if (hostAddress.startsWith("192.")) {
                            target = hostAddress;
                        }
                    }
                    sb.append(hostAddress).append("\n");
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        Log.i(TAG, " -> get all inet_address is: " + sb.toString() + "\n\t target is = " + target);
        return target;
    }

    private static String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "."
                + (0xFF & paramInt >> 8) + "."
                + (0xFF & paramInt >> 16) + "."
                + (0xFF & paramInt >> 24);
    }
}
