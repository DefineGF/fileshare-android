package com.cjm.fileshare.widget;

import android.content.Context;

import com.cjm.fileshare.R;
import com.cjm.fileshare.bean.FileInfo;

import java.util.List;
import java.util.Objects;

public class FileDialogHelper {
    private static final String TAG = "FileDialogHelper";

    private FileDialog fileDialog;
    private Context    context;
    private int        style;
    private List<FileInfo> fileInfoList = null;

    private OnSelectedHelperListener selectedHelperListener;

    private FileDialogHelper(Context context) {
        this.context = context;
    }

    public static FileDialogHelper create(Context context) {
        return new FileDialogHelper(context);
    }

    public FileDialogHelper setFileInfoList(List<FileInfo> list) {
        this.fileInfoList = list;
        return this;
    }

    public FileDialogHelper setSelectedListener(OnSelectedHelperListener listener) {
        this.selectedHelperListener = listener;
        return this;
    }

    public FileDialogHelper setStyle(int style) {
        this.style = style;
        return this;
    }

    public void show() {
        fileDialog = new FileDialog(context, fileInfoList, style, new FileDialog.OnFileDialogButtonListener() {
            @Override
            public void confirmSelectedData(List<FileInfo> selectedList) {
                selectedHelperListener.onGetSelectedFileInfoList(selectedList);
                fileDialog.dismiss();
            }

            @Override
            public void cancel() {
                fileDialog.dismiss();
            }
        });
        fileDialog.setCancelable(false);
        Objects.requireNonNull(fileDialog.getWindow()).setBackgroundDrawableResource(R.color.colorTransparent);
        fileDialog.show();
    }

    /**
     * 向 Activity 回调选中的 FileInfo;
     * 由 确定 按钮触发;
     */
    public interface OnSelectedHelperListener {
        void onGetSelectedFileInfoList(List<FileInfo> fileInfoList);
    }
}
