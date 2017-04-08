package jp.gr.java_conf.falius.andcommuni.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import jp.gr.java_conf.falius.communication.core.Client;
import jp.gr.java_conf.falius.communication.core.SwapClient;
import jp.gr.java_conf.falius.communication.listener.OnDisconnectCallback;
import jp.gr.java_conf.falius.communication.listener.OnReceiveListener;
import jp.gr.java_conf.falius.communication.listener.OnSendListener;
import jp.gr.java_conf.falius.communication.rcvdata.ReceiveData;
import jp.gr.java_conf.falius.communication.senddata.SendData;
import jp.gr.java_conf.falius.communication.swapper.OnceSwapper;
import jp.gr.java_conf.falius.communication.swapper.Swapper;

/**
 * Created by ymiyauchi on 2017/04/04.
 *
 * <p>
 * Bluetooth通信を行うクライアントを表すクラスです。
 */
public class BluetoothClient implements SwapClient {
    private static final String TAG = BluetoothClient.class.getName();
    private final BluetoothDevice mDevice;
    private final String mUuid;
    private final String mRemoteAddress;
    private final Swapper mSwapper;

    private ExecutorService mExecutorService = null;

    private OnSendListener mOnSendListener = null;
    private OnReceiveListener mOnReceiveListener = null;
    private OnDisconnectCallback mOnDisconnectCallback = null;
    private Client.OnConnectListener mOnConnectListener = null;
    private volatile boolean mDoContinue = true;

    public BluetoothClient(BluetoothDevice device, String uuid) {
        this(device, uuid, null);
    }

    public BluetoothClient(BluetoothDevice device, String uuid, Swapper swapper) {
        mDevice = device;
        mUuid = uuid;
        mRemoteAddress = device.getAddress();
        mSwapper = swapper;
    }

    @Nullable
    @Override
    public ReceiveData send(final SendData sendData) throws IOException, TimeoutException {
        return start(new OnceSwapper() {
            @Override
            public SendData swap(String s, ReceiveData receiveData) throws Exception {
                return sendData;
            }
        });
    }

    @Nullable
    @Override
    public ReceiveData start(@NonNull Swapper swapper) throws IOException, TimeoutException {
        try (BluetoothSocket socket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString(mUuid))) {
            socket.connect();
            Log.i(TAG, "success connect");
            if (mOnConnectListener != null) {
                mOnConnectListener.onConnect(mRemoteAddress);
            }

            try (InputStream is = socket.getInputStream(); OutputStream os = socket.getOutputStream()) {
                ReceiveData receiveData = null;
                while (mDoContinue) {
                    SendData sendData = swapper.swap(mRemoteAddress, receiveData);
                    if (sendData == null) {
                        break;
                    }
                    new WritingHandler(mRemoteAddress, os, sendData, mOnSendListener).send();
                    receiveData = new ReadingHandler(mRemoteAddress, is, mOnReceiveListener).receive();
                    if (!swapper.doContinue() || receiveData == null) {
                        break;
                    }
                }
                if (mOnDisconnectCallback != null) {
                    mOnDisconnectCallback.onDissconnect(mRemoteAddress, null);
                }
                return receiveData;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (mOnDisconnectCallback != null) {
                mOnDisconnectCallback.onDissconnect(mRemoteAddress, e);
            }
            return null;
        }
    }

    @Override
    public Future<ReceiveData> startOnNewThread() {
        if (mSwapper == null) {
            throw new IllegalStateException("don't pass Swapper to constructor");
        }
        if (mExecutorService == null) {
            synchronized (this) {
                if (mExecutorService == null) {
                    mExecutorService = Executors.newCachedThreadPool();
                }
            }
        }
        Log.d(TAG, "start on new thread");
        return mExecutorService.submit(this);
    }


    @Override
    public void addOnSendListener(OnSendListener onSendListener) {
        mOnSendListener = onSendListener;
    }

    @Override
    public void addOnReceiveListener(OnReceiveListener onReceiveListener) {
        mOnReceiveListener = onReceiveListener;
    }

    @Override
    public void addOnDisconnectCallback(OnDisconnectCallback onDisconnectCallback) {
        mOnDisconnectCallback = onDisconnectCallback;
    }

    @Override
    public void addOnConnectListener(Client.OnConnectListener listener) {
        mOnConnectListener = listener;
    }

    @Override
    public void close() throws IOException {
        mDoContinue = false;
        if (mExecutorService != null) {
            mExecutorService.shutdownNow();
        }
        if (mOnDisconnectCallback != null) {
            mOnDisconnectCallback.onDissconnect(mRemoteAddress, null);
        }
    }


    @Override
    public ReceiveData call() throws Exception {
        if (mSwapper == null) {
            throw new IllegalStateException("don't pass Swapper to constructor");
        }
        return start(mSwapper);
    }

}
