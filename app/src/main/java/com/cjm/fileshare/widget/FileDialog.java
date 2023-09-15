package com.cjm.fileshare.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cjm.fileshare.R;
import com.cjm.fileshare.adapter.FileDialogAdapter;
import com.cjm.fileshare.bean.FileInfo;

import java.util.ArrayList;
import java.util.List;

public class FileDialog extends Dialog implements FileDialogAdapter.OnFileItemClickListener {
    private List<FileInfo> fileInfoList;
    private List<FileInfo> fileSelectedList = new ArrayList<>();

    private OnFileDialogButtonListener submitListener;
    private int style; // 0 单选

    public FileDialog(Context context, List<FileInfo> fileInfoList, int style, OnFileDialogButtonListener listener) {
        super(context);
        this.fileInfoList   = fileInfoList;
        this.submitListener = listener;
        this.style          = style;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.dialog_file_list);
        this.setCanceledOnTouchOutside(false);
        initView();
    }

    private void initView() {
        // 添加 FileDialogAdapter
        FileDialogAdapter fileDialogAdapter = new FileDialogAdapter(fileInfoList, style);
        fileDialogAdapter.setOnFileItemClickListener(this);

        // 添加 RecyclerView
        RecyclerView fileRecyclerView = findViewById(R.id.rv_selector);
        fileRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fileRecyclerView.setAdapter(fileDialogAdapter);

        // 确定按钮
        findViewById(R.id.btn_ok).setOnClickListener(view -> {
            for (FileInfo fileInfo : fileInfoList) {
                if (fileInfo.isSelected()) {
                    fileSelectedList.add(fileInfo);
                }
            }
            submitListener.confirmSelectedData(fileSelectedList);
        });
        // 取消按钮
        findViewById(R.id.btn_cancel).setOnClickListener(view -> submitListener.cancel());
    }


    @Override
    public void onFileItemClick(int pos) {
        if (style == 1) { // 多选模式
            FileInfo t = fileInfoList.get(pos);
            t.setSelected(!t.isSelected());
        } else {         // 单选模式
            for (int i = 0; i < fileInfoList.size(); i++) {
                if (i == pos) {
                    if (!fileInfoList.get(i).isSelected()) {
                        fileInfoList.get(i).setSelected(true);
                    }
                } else {
                    fileInfoList.get(i).setSelected(false);
                }
            }
        }
    }

    /**
     * 用于监听 Dialog 中的 确定/取消 按钮
     */
    public interface OnFileDialogButtonListener {
        void confirmSelectedData(List<FileInfo> selectedList);
        void cancel();
    }
}
