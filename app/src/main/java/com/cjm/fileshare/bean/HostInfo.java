package com.cjm.fileshare.bean;

import androidx.annotation.NonNull;

public class HostInfo {
    private String name;        // 记录连接用户的名字
    private String ip;          // 客户端 ip 地址
    private int  port;          // 客户端 端口


    public HostInfo(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @NonNull
    @Override
    public String toString() {
        return "name:" + name + ",ip:" + ip + ",port:" + port;
    }

    /**
     * 从字符串中创建 HostInfo 对象
     * @param content 字符串内容
     * @return HostInfo 对象
     */
    public static HostInfo createHostInfoByStr(String content) {
        String[] infos = content.split(",");
        String name = infos[0].substring(infos[0].indexOf(":") + 1);
        String ip   = infos[1].substring(infos[1].indexOf(":") + 1);
        int    port = Integer.parseInt(infos[2].substring(infos[2].indexOf(":") + 1));

        return new HostInfo(name, ip, port);
    }

    public HostInfo copy() {
        return new HostInfo(this.getName(), this.getIp(), this.getPort());
    }
}
