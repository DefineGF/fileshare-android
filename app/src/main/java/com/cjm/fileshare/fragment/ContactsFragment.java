package com.cjm.fileshare.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cjm.fileshare.R;
import com.cjm.fileshare.activity.LocalShareActivity;
import com.cjm.fileshare.adapter.ContactsAdapter;
import com.cjm.fileshare.bean.FileInfo;
import com.cjm.fileshare.bean.HostInfo;
import com.cjm.fileshare.widget.FileDialogHelper;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FileUploadListener} interface
 * to handle interaction events.
 * Use the {@link ContactsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContactsFragment extends Fragment {
    private static final String TAG = "ContactsFragment";

    private Context mContext = null;
    private FileUploadListener fileUploadListener = null;       // 监听 FloatingActionButton 文件发送事件

    private FileDialogHelper  fileDialogHelper    = null;       // 文件选择框
    private ContactsAdapter   contactsAdapter     = null;       // 在线用户适配器
    private List<HostInfo>    hostInfoList        = null;       // 在线用户数据
    private List<FileInfo>    fileInfoList        = null;       // 发送文件夹文件数据
    private String            selectedFileName    = null;       // FileDialog 选中的需要传送的 FileInfo
    private String            selectedTargetName  = null;       // 需要传送的目标名字

    public ContactsFragment() {
        Log.i(TAG, "default constructor");
    }

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");
        this.mContext      = context;
        hostInfoList       = ((LocalShareActivity)context).getUsers();
        fileInfoList       = ((LocalShareActivity)context).getFiles();
        fileUploadListener = (FileUploadListener) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 创建 FileDialogHelper
        fileDialogHelper = FileDialogHelper.create(mContext).setFileInfoList(fileInfoList).setSelectedListener(fileInfoList -> {
            if (fileInfoList.size() > 0) {
                selectedFileName = fileInfoList.get(0).getFileName();
            }
        });

        // 创建ContactsAdapter: 添加Item点击监听:
        contactsAdapter = new ContactsAdapter(mContext, hostInfoList, target -> {
            selectedTargetName = target; // 文件接收者名字
            fileDialogHelper.show();     // 文件选择框显示
        });
        Log.i(TAG, "onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        // 创建 RecyclerView 视图
        RecyclerView recyclerView = view.findViewById(R.id.contacts_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.setAdapter(contactsAdapter);

        // 为 FloatingActionButton 添加监听
        view.findViewById(R.id.fb_file_upload).setOnClickListener(view1 -> {
            if (selectedTargetName != null && selectedFileName != null) {
                fileUploadListener.onFileUploadClick(selectedFileName, selectedTargetName);
                Log.i(TAG, "get the target_name = " + selectedTargetName + "; file_name = " + selectedFileName);
            } else {
                Toast.makeText(mContext, "请选择发送文件!", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "发送信息获取失败");
            }
        });
        return view;
    }

    /**
     * 更新 联系人 recycler_view 视图
     */
    public void contactsDataUpdate() {
        contactsAdapter.notifyDataSetChanged();
        Log.i(TAG, "数据更新, 当前数据信息: " + hostInfoList.toString());
    }


    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");
        fileUploadListener = null;
    }


    /**
     * 监听 FloatingActionButton 按钮的文件上传按钮
     */
    public interface FileUploadListener {
        void onFileUploadClick(String selectedFileName, String targetName);
    }
}
