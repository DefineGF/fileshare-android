package com.cjm.fileshare.listener;

import com.cjm.fileshare.bean.HostInfo;

import java.util.List;

public interface TCPInteractionListener {
    /**
     * 客户端连接 server 成功
     * @param serverInfo 服务端信息
     */
    void onConnSuccess(HostInfo serverInfo);

    /**
     * 接收到普通消息 (display on text_view)
     * @param msg 消息实体
     */
    void onInfoNotify(String msg);

    /**
     * 客户端加入
     * @param clientInfo 新客户端信息
     */
    void onClientJoin(HostInfo clientInfo);

    /**
     * 客户端离开
     * @param clientName 客户端名字
     */
    void onClientExit(String clientName);


    /**
     * 文件发送开始提醒
     * @param fileName 文件名
     */
    void onFileSendBegin(String fileName);

    /**
     * 文件发送结束提醒
     * @param fileName 文件名
     */
    void onFileSendEnd(String fileName);

    /**
     * 文件接收开始提醒
     * @param fileName 文件名
     */
    void onFileSaveBegin(String fileName);

    /**
     * 文件接收结束
     * @param fileName 文件名
     */
    void onFileSaveEnd(String fileName);
}
