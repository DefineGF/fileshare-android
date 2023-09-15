package com.cjm.fileshare.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cjm.fileshare.R;
import com.cjm.fileshare.bean.HostInfo;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
    private static final String TAG = "ContactsAdapter";

    private Context        context;
    private List<HostInfo> hostInfoList;

    private OnContactsItemListener contactsItemListener;

    public ContactsAdapter( Context context, List<HostInfo> users, OnContactsItemListener contactsItemListener) {
        this.hostInfoList = users;
        this.context = context;
        this.contactsItemListener = contactsItemListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_contacts, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HostInfo hostInfo = hostInfoList.get(position);
        holder.tvName.setText(hostInfo.getName());
        holder.tvIp.setText(hostInfo.getIp());
        holder.tvPort.setText(String.valueOf(hostInfo.getPort())); // 需要将 int 类型转换为 String
        holder.itemView.setOnClickListener(view -> {
            contactsItemListener.onContactsItemClick(hostInfo.getName());
        });
    }

    @Override
    public int getItemCount() {
        return hostInfoList.size();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIp, tvPort;
        View itemView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvName = itemView.findViewById(R.id.item_tv_name);
            tvIp   = itemView.findViewById(R.id.item_tv_ip);
            tvPort = itemView.findViewById(R.id.item_tv_port);
        }
    }

    public interface OnContactsItemListener {
        void onContactsItemClick(String target);
    }
}
