package jp.gr.java_conf.falius.andcommuni.core;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import jp.gr.java_conf.falius.communication.core.Client;
import jp.gr.java_conf.falius.communication.core.JITClient;
import jp.gr.java_conf.falius.communication.listener.OnDisconnectCallback;
import jp.gr.java_conf.falius.communication.listener.OnReceiveListener;
import jp.gr.java_conf.falius.communication.listener.OnSendListener;
import jp.gr.java_conf.falius.communication.rcvdata.ReceiveData;
import jp.gr.java_conf.falius.communication.senddata.SendData;
import jp.gr.java_conf.falius.communication.swapper.RepeatSwapper;
import jp.gr.java_conf.falius.communication.swapper.Swapper;

/**
 * Created by ymiyauchi on 2017/04/05.
 */

public class BluetoothJITClient implements JITClient {
    private static final String TAG = BluetoothJITClient.class.getName();
    private final BluetoothClient mClient;
    private final BlockingQueue<SendData> mSendDataQueue = new LinkedBlockingQueue<>();

    public BluetoothJITClient(BluetoothDevice device, String uuid, OnReceiveListener receiveListener) {
        mClient = new BluetoothClient(device, uuid, createSwapper());
        mClient.addOnReceiveListener(receiveListener);

    }

    private Swapper createSwapper() {
        return new RepeatSwapper() {
            @Override
            public SendData swap(String s, ReceiveData receiveData) throws Exception {
                Log.d(TAG, "swap");
                try {
                    return mSendDataQueue.take();
                } catch (InterruptedException e) {
                    mClient.close();
                }
                return null;
            }
        };
    }

    @Override
    public void send(SendData sendData) throws IOException, TimeoutException {
        mSendDataQueue.add(sendData);
    }

    @Override
    public Future<ReceiveData> startOnNewThread() {
        return mClient.startOnNewThread();
    }

    @Override
    public void addOnSendListener(OnSendListener onSendListener) {
        mClient.addOnSendListener(onSendListener);
    }

    @Override
    public void addOnReceiveListener(OnReceiveListener onReceiveListener) {
        mClient.addOnReceiveListener(onReceiveListener);
    }

    @Override
    public void addOnDisconnectCallback(OnDisconnectCallback onDisconnectCallback) {
        mClient.addOnDisconnectCallback(onDisconnectCallback);
    }

    @Override
    public void addOnConnectListener(Client.OnConnectListener listener) {
        mClient.addOnConnectListener(listener);
    }

    @Override
    public void close() throws IOException {
        mClient.close();
    }

    @Override
    public ReceiveData call() throws Exception {
        return mClient.call();
    }
}
