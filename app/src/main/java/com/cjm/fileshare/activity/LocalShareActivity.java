package com.cjm.fileshare.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import com.cjm.fileshare.R;
import com.cjm.fileshare.bean.FileInfo;
import com.cjm.fileshare.bean.HostInfo;
import com.cjm.fileshare.bean.InetInfo;
import com.cjm.fileshare.config.InetConfig;
import com.cjm.fileshare.config.MessageConfig;
import com.cjm.fileshare.config.SystemConfig;
import com.cjm.fileshare.fragment.ContactsFragment;
import com.cjm.fileshare.fragment.MessageFragment;
import com.cjm.fileshare.listener.TCPInteractionListener;
import com.cjm.fileshare.tcp.Client;
import com.cjm.fileshare.tcp.Server;
import com.cjm.fileshare.util.FileUtil;
import com.cjm.fileshare.util.InternetUtil;

import java.util.LinkedList;
import java.util.List;

public class LocalShareActivity extends AppCompatActivity implements View.OnClickListener,
        MessageFragment.OnFragmentInteractionListener, ContactsFragment.FileUploadListener {
    private static final String TAG = "LocalShareActivity";

    private ContactsFragment  contactsFragment = null;
    private MessageFragment   messageFragment  = null;
    private Toolbar           toolbar          = null;
    private TextView          messageTv        = null;
    private TextView          contactsTv       = null;

    private Handler handler = null;   // 用于子线程处理 UI
    private Server  server  = null;
    private Client  client  = null;
    private TCPInteractionListener tcpInteractionListener;

    private boolean isShowMessage = true;   // 当前显示画面标志
    private boolean isAsClient    = false;  // 当前工作模式标志

    private List<HostInfo> users = null;    // 所有在线信息（第一个是用户本身）; onCreate() 中初始化
    private List<FileInfo> files = null;    // 发送文件夹 下的文件内容

    private InetInfo inetInfo    = null;    // 当前主机网络信息
    private String   userName    = "self";  // 默认用户信息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_share);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initView();         // 初始化视图
        updateInetInfo();   // 更新网络信息
        tcpInteractionListener = new MyTCPInteractionListener();
        handler = new Handler();
        users   = new LinkedList<>();
    }

    private void initView() {
        initToolbar();
        messageTv = findViewById(R.id.local_tv_message);
        contactsTv = findViewById(R.id.local_tv_contacts);
        messageTv.setOnClickListener(this);
        contactsTv.setOnClickListener(this);
        toMessageFragment();  // 默认打开 message_fragment
        registerDialogShow(); // 信息键入弹窗
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.local_tv_message) {         // 切换到 message
            if (!isShowMessage) {
                toMessageFragment();
            }
        } else if (v.getId() == R.id.local_tv_contacts) { // 切换到 contacts
            if (isShowMessage) {
                toContactsFragment();
            }
        }
    }

    private void startServer() {
        HostInfo serverInfo = new HostInfo(userName, inetInfo.getIpSelf(), InetConfig.SERVER_PORT);
        server = new Server(serverInfo, tcpInteractionListener);
        server.start();
        users.add(serverInfo);          // 服务器将本身信息放入用户列表
        Log.i(TAG, "服务器启动!");
    }

    private void startClient() {
        HostInfo hostInfo = new HostInfo(userName, inetInfo.getIpSelf(), 0);
        client = new Client(hostInfo, inetInfo.getIpWifi(), tcpInteractionListener); // target_ip 是 wifi ip
        client.start();
        users.add(hostInfo);           // 客户端将本身信息放入用户列表
        Log.i(TAG, "客户端启动!");
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "退回键:");
        if (isAsClient && client != null) {
            new Thread(() -> {
                client.exit();  // 需要向服务端发送 exit (因此需要在线程中，防止 NetworkOnMainThreadException)
                client = null;  // 内存回收
            }).start();
        }
        if (!isAsClient && server != null){
            server.exit();
            server = null;
        }
        users.clear();
        users = null;
        Log.i(TAG, "退出成功！");
        this.finish();
    }

    private void initToolbar() {
        toolbar.setTitle("局域网文件共享");
        toolbar.setNavigationIcon(R.drawable.refresh);

        toolbar.setNavigationOnClickListener(view -> {  // 导航栏刷新图标事件
            if (isShowMessage) {
                updateInetInfo();
                Toast.makeText(LocalShareActivity.this, "网络信息已更新", Toast.LENGTH_SHORT).show();
            } else {
                contactsFragment.contactsDataUpdate();
                Toast.makeText(LocalShareActivity.this, "更新用户信息", Toast.LENGTH_SHORT).show();
            }
        });

        toolbar.setOnMenuItemClickListener(item -> {  // 导航栏目录事件
            switch (item.getItemId()) {
                case R.id.menu_help:
                    Log.i(TAG, "ask for help");
                    showInfoDialog("帮助", MessageConfig.getCmdContent());
                    break;
                case R.id.menu_internet: // 更新网络信息
                    Log.i(TAG, "show internet info");
                    updateInetInfo();
                    String content = "\t用户昵称:" + userName + "\n" + inetInfo.toString();
                    showInfoDialog("网络信息", content);
                    break;
            }
            return true;
        });
    }

    /**
     * 启动时用于输入用户配置的 Dialog
     */
    private void registerDialogShow() {
        @SuppressLint("InflateParams")
        View registerDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register_content, null);
        final EditText etName = registerDialogView.findViewById(R.id.dialog_et_name);
        RadioGroup radioGroup = registerDialogView.findViewById(R.id.dialog_rg_model);
        radioGroup.setOnCheckedChangeListener((radioGroup1, i) -> {
            if (i == R.id.dialog_rb_client) {
                isAsClient = true;
            } else if (i == R.id.dialog_rb_server) {
                isAsClient = false;
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("信息")
                .setCancelable(false)
                .setView(registerDialogView)
                .setPositiveButton("连接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        userName = etName.getText().toString();
                        Log.i(TAG, "userName = " + userName + "; isAsClient = " + isAsClient);
                        if (isAsClient) {
                            startClient();
                        } else {
                            startServer();
                        }
                    }
                }).show();
    }


    /**
     * 更新网络信息: 本机网络类型 & 本机IP & 热点IP
     */
    private void updateInetInfo() {
        if (inetInfo == null) {
            inetInfo = new InetInfo();
        }
        inetInfo.setInetType(InternetUtil.getNetType(LocalShareActivity.this));
        inetInfo.setIpSelf(InternetUtil.getLocalIp(LocalShareActivity.this));
        inetInfo.setIpWifi(InternetUtil.getWifiIp(LocalShareActivity.this));
        Log.i(TAG, "获取网络信息为：" + inetInfo.toString());
    }

    /**
     * 更新 文件数据源
     */
    private void updateFileInfo() {
        if (files != null) {
            files.clear();
            files = null;
        }
        files = FileUtil.getFileInfoList(SystemConfig.SEND_DIR_PATH);
    }


    /**
     * MessageFragment 中 发送消息 的按钮监听事件
     * @param msg EditText 输入内容
     */
    @Override
    public void onMsgSendInActivity(final String msg) {
        if (isAsClient && client != null) {    // 客户端发送消息
            new Thread(() -> client.onSendInfo(msg)).start();
            Log.i(TAG, "向服务端发送消息:" + msg);
        }
        if (!isAsClient && server != null) {  // 服务端发送消息
            new Thread(() -> server.onSendInfo(msg, true)).start();
        }
    }

    /**
     * ContactsFragment 中的文件上传 FloatingActionButton 监听事件
     * @param fileName 需要传送的文件
     * @param targetName 需要传送的目标 名字
     */
    @Override
    public void onFileUploadClick(String fileName, String targetName) {
        new Thread(() -> {
            if (isAsClient) {
                client.onSendFile(SystemConfig.SEND_DIR_PATH + "/" + fileName, targetName);
            } else {
                server.onSendFile(SystemConfig.SEND_DIR_PATH + "/" + fileName, targetName);
            }
        }).start();
    }

    /**
     * 用于核心TCP进程与 Activity 交互
     */
    private class MyTCPInteractionListener implements TCPInteractionListener {

        @Override
        public void onConnSuccess(HostInfo serverInfo) {
            users.add(serverInfo);
            handler.post(() -> Toast.makeText(LocalShareActivity.this, "连接 " + serverInfo.getName() + " 成功!", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onInfoNotify(final String msg) {
            if (messageFragment != null) {
                handler.post(() -> messageFragment.setDisplayTvString(msg));
                Log.i(TAG, "显示内容: " + msg);
            }
        }

        /**
         * 客户端接入
         * @param clientInfo 客户端信息 HostInfo
         */
        @Override
        public void onClientJoin(final HostInfo clientInfo) {
            Log.i(TAG, "新节点:" + clientInfo.toString());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LocalShareActivity.this, "节点:" + clientInfo.getName() + " 连接", Toast.LENGTH_LONG).show();
                }
            });
            users.add(clientInfo.copy());
            Log.i(TAG, "当前节点信息:" + users.toString());
        }

        /**
         * 客户端离开
         * @param clientName 离开的客户端的名字
         */
        @Override
        public void onClientExit(final String clientName) {
            Log.i(TAG, "节点: " + clientName + " 离开");
            handler.post(() -> Toast.makeText(LocalShareActivity.this, "节点:" + clientName + " 离开", Toast.LENGTH_LONG).show());
            if (server != null) { // 或许服务端提前离开
                users.removeIf(hostInfo -> hostInfo.getName().equals(clientName));
                Log.i(TAG, "当前节点信息: " + users.toString());
            }
        }

        @Override
        public void onFileSendBegin(String fileName) {
            handler.post(() -> Toast.makeText(LocalShareActivity.this, fileName + " 文件发送开始！", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onFileSendEnd(String fileName) {
            handler.post(() -> Toast.makeText(LocalShareActivity.this, fileName + " 文件发送结束!", Toast.LENGTH_LONG).show());
        }

        @Override
        public void onFileSaveBegin(String fileName) {
            handler.post(() -> Toast.makeText(LocalShareActivity.this, fileName + " 文件接收开始~", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onFileSaveEnd(String fileName) {
            handler.post(() -> Toast.makeText(LocalShareActivity.this, fileName + " 文件接收结束!", Toast.LENGTH_SHORT).show());
        }
    }


    //--------------------------------------------- switch fragment start----------------------------------------------------------------
    private void toMessageFragment() {
        if (messageFragment == null) {
            messageFragment = MessageFragment.newInstance("");
        }
        if (isShowMessage) {
            if (!messageFragment.isAdded()) { // 首次启动可能未添加进去
                getSupportFragmentManager().beginTransaction().add(R.id.local_fragment_container, messageFragment).commit();
                messageTv.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bottom_tv_selected, null));
            }
        } else {
            getSupportFragmentManager().beginTransaction().hide(contactsFragment).show(messageFragment).commit();
            contactsTv.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bottom_tv_unselected, null));
            messageTv.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bottom_tv_selected, null));
            isShowMessage = true;
        }
    }

    private void toContactsFragment() {
        if (contactsFragment == null) {
            contactsFragment = ContactsFragment.newInstance();
        }
        if (isShowMessage) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            if (!contactsFragment.isAdded()) {
                fragmentTransaction.add(R.id.local_fragment_container, contactsFragment);
            }
            fragmentTransaction.hide(messageFragment).show(contactsFragment).commit();
            messageTv.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bottom_tv_unselected, null));
            contactsTv.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bottom_tv_selected, null));
            isShowMessage = false;
        }
    }
    //----------------------------------------------------switch fragment end----------------------------------------------------------

    /**
     * 开启普通消息提示弹窗
     * @param title dialog 标题
     * @param info dialog  内容 (可复制)
     */
    private void showInfoDialog(String title, String info) {
        TextView textView = new TextView(LocalShareActivity.this);
        textView.setText(info);
        textView.setTextSize(17);
        textView.setTextIsSelectable(true);
        textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(textView)
                .setPositiveButton("确定", null)
                .show();
    }

    // 加载导航栏 menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_local_share, menu);
        return true;
    }

    public List<HostInfo> getUsers() {
        return users;
    }

    public List<FileInfo> getFiles() {
        updateFileInfo();
        return files;
    }

}
