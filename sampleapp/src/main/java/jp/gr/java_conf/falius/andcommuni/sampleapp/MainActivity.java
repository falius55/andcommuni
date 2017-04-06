package jp.gr.java_conf.falius.andcommuni.sampleapp;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.gr.java_conf.falius.andcommuni.BluetoothJITClient;
import jp.gr.java_conf.falius.andcommuni.ConnectingDialog;
import jp.gr.java_conf.falius.andcommuni.DeviceSelectDialog;
import jp.gr.java_conf.falius.andcommuni.DeviceSelector;
import jp.gr.java_conf.falius.andcommuni.OnUIConnectListener;
import jp.gr.java_conf.falius.andcommuni.OnUIReceiveListener;
import jp.gr.java_conf.falius.communication.core.JITClient;
import jp.gr.java_conf.falius.communication.listener.OnDisconnectCallback;
import jp.gr.java_conf.falius.communication.rcvdata.ReceiveData;
import jp.gr.java_conf.falius.communication.senddata.BasicSendData;
import jp.gr.java_conf.falius.communication.senddata.SendData;

public class MainActivity extends AppCompatActivity implements DeviceSelector {
    private static final String TAG = MainActivity.class.getName();
    private static final String UUID = "97d38833-e31a-4a71-8d8e-4e44d052ce2b";

    @BindString(R.string.connect_state)
    String CONNECT_STATE;
    @BindString(R.string.disconnect_state)
    String DISCONNECT_STATE;

    @BindView(R.id.editText1)
    EditText mInputEdit;
    @BindView(R.id.editText2)
    EditText mResultEdit;
    @BindView(R.id.txt_state)
    TextView mTxtState;

    private JITClient mClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.btn_send)
    void onClickSend() {
        String msg = mInputEdit.getText().toString();
        if (mClient != null) {
            SendData sendData = new BasicSendData();
            sendData.put(msg);
            try {
                mClient.send(sendData);
            } catch (IOException | TimeoutException e) {
                Log.e(TAG, "send error", e);
            }
        }
    }

    @OnClick(R.id.btn_reset)
    void onClickReset() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    public void onSelect(BluetoothDevice device) {
        Log.d(TAG, "on select");
        final ConnectingDialog prog = new ConnectingDialog();
        prog.show(getSupportFragmentManager(), "connecting");

        mClient = new BluetoothJITClient(device, UUID, new OnUIReceiveListener(Looper.myLooper()) {
            @Override
            public void onUIReceive(String s, ReceiveData receiveData) {
                String result = receiveData.getString();
                mResultEdit.setText(result);
            }
        });
        mClient.addOnDisconnectCallback(new OnDisconnectCallback() {
            @Override
            public void onDissconnect(String s, Throwable throwable) {
                Log.d(TAG, "disconnect", throwable);
                mTxtState.setText(DISCONNECT_STATE);
            }
        });
        mClient.addOnConnectListener(new OnUIConnectListener(Looper.myLooper()) {
            @Override
            public void onUIConnect(String s) {
                prog.dismiss();
                mTxtState.setText(CONNECT_STATE);
            }
        });
        mClient.startOnNewThread();
        Log.d(TAG, "end on select");
    }

    @Override
    public void onResume() {
        super.onResume();
        DialogFragment dialog = DeviceSelectDialog.newInstance();
        dialog.show(getSupportFragmentManager(), "device dialog");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mClient != null) {
            try {
                mClient.close();
                mClient = null;
            } catch (IOException e) {
                Log.e(TAG, "client can not close", e);
            }
        }
    }
}
