package com.cjm.fileshare.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cjm.fileshare.R;
import com.cjm.fileshare.bean.FileInfo;

import java.util.List;

public class FileDialogAdapter extends RecyclerView.Adapter<FileDialogAdapter.ViewHolder>{
    private List<FileInfo> fileInfoList;
    private final int style;
    private OnFileItemClickListener onFileItemClickListener;

    private ViewHolder selectedHolder;

    public FileDialogAdapter(List<FileInfo> list, int style) {
        fileInfoList = list;
        this.style   = style;
    }

    public void setOnFileItemClickListener(OnFileItemClickListener listener) {
        this.onFileItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_dialog, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileInfo fileInfo = fileInfoList.get(position);
        holder.tvName.setText(fileInfo.getFileName());
        holder.tvSize.setText(fileInfo.getFileSize());
        holder.box.setChecked(fileInfo.isSelected());
        holder.box.setOnClickListener(view -> {
            if (style == 1) { // 多选模式
                onFileItemClickListener.onFileItemClick(position);
            } else {          // 单选模式
                if (selectedHolder == null) {
                    selectedHolder = holder;
                } else {
                    selectedHolder.box.setChecked(false);
                    selectedHolder = holder;
                }
                onFileItemClickListener.onFileItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileInfoList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSize;
        CheckBox box;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvSize = itemView.findViewById(R.id.tv_size);
            box    = itemView.findViewById(R.id.box);
        }
    }

    public interface OnFileItemClickListener {
        void onFileItemClick(int pos);
    }
}
