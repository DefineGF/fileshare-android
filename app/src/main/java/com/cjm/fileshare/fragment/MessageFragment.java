package com.cjm.fileshare.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.cjm.fileshare.R;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MessageFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MessageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessageFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "MessageFragment";

    private static final String ARG_TV = "TV_PAR";

    private Handler handler = null;

    private TextView displayTv;
    private EditText inputEt;
    private String   displayTvString = null;

    private OnFragmentInteractionListener mListener;

    public MessageFragment() {
        Log.i(TAG, "default constructor");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tvStr Parameter 1.
     * @return A new instance of fragment MessageFragment.
     */
    public static MessageFragment newInstance(String tvStr) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TV, tvStr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        handler = new Handler();
        if (getArguments() != null) {
            displayTvString = getArguments().getString(ARG_TV);
            Log.i(TAG, "get the saved message: " + displayTvString);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        view.setOnClickListener(this);
        displayTv = view.findViewById(R.id.message_tv_message);
        displayTv.setMovementMethod(ScrollingMovementMethod.getInstance());  // 设置TextView 的 Scroll
        displayTv.setOnClickListener(this);
        displayTv.setText((displayTvString == null ? "消息内容" : displayTvString));

        view.findViewById(R.id.message_btn_send).setOnClickListener(this);   // 发送按钮设置监听
        inputEt = view.findViewById(R.id.message_et_input);
        inputEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    Objects.requireNonNull(getActivity()).findViewById(R.id.ll_bottom).setVisibility(View.GONE);
                } else {
                    Objects.requireNonNull(getActivity()).findViewById(R.id.ll_bottom).setVisibility(View.VISIBLE);
                }
            }
        });
        return view;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.message_btn_send) {            // 发送消息按钮
            String input = inputEt.getText().toString();
            if (!"".equals(input)) {
                inputEt.setText("");
                this.setDisplayTvString("I: " + input);
                mListener.onMsgSendInActivity(input);
            }
        } else if (view.getId() == R.id.message_tv_message) {  // 清空 edit_text 的 焦点
            inputEt.clearFocus();
        }
    }

    public void setDisplayTvString(final String content) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                displayTv.append(content + "\n");
            }
        });

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement FileUploadListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onDestroyView() {
        String msg = displayTv.getText().toString();
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TV, msg);
        fragment.setArguments(args);

        super.onDestroyView();
        Log.i(TAG, "onDestroyView, save the message: " + msg);
    }

    public interface OnFragmentInteractionListener {
        void onMsgSendInActivity(String msg);
    }
}
