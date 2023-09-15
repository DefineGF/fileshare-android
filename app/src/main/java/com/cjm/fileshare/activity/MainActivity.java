package com.cjm.fileshare.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.cjm.fileshare.R;
import com.cjm.fileshare.config.SystemConfig;
import com.cjm.fileshare.util.FileUtil;
import com.cjm.fileshare.util.PermissionUtil;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        PermissionUtil.myRequestWritePermission(this);
    }

    private void initView() {
        findViewById(R.id.main_btn_local).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
       if (v.getId() == R.id.main_btn_local){
            startActivity(new Intent(MainActivity.this, LocalShareActivity.class));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PermissionUtil.WRITE_REQUEST) {
            if(permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // 同意

                PermissionUtil.WRITE_AGREE = true;

                if (FileUtil.createDirInRoot(SystemConfig.SEND_DIR_PATH)) { // 创建 "发送文件夹"
                    Log.i(TAG, "发送文件夹: " + SystemConfig.SEND_DIR_PATH + " 创建成功!");
                }
                if (FileUtil.createDirInRoot(SystemConfig.SAVE_DIR_PATH)) { // 创建 "接收文件夹"
                    Log.i(TAG, "接收文件夹: " + SystemConfig.SAVE_DIR_PATH + " 创建成功!");
                }
            } else {
                Toast.makeText(this,"你不同意那我就没办法了！",Toast.LENGTH_SHORT).show();
            }
        }
    }
}
