package jp.gr.java_conf.falius.andcommuni.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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

    private final Set<Session> mSessionSet = Collections.synchronizedSet(new HashSet<Session>());
    private ExecutorService mExecutorService = null;

    private OnSendListener mOnSendListener = null;
    private OnReceiveListener mOnReceiveListener = null;
    private OnDisconnectCallback mOnDisconnectCallback = null;
    private Client.OnConnectListener mOnConnectListener = null;

    public BluetoothClient(BluetoothDevice device, String uuid) {
        this(uuid, device, null);
    }

    public BluetoothClient(String uuid, BluetoothDevice device, Swapper swapper) {
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
        Objects.requireNonNull(swapper);
        try (BluetoothSocket socket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString(mUuid))) {
            socket.connect();
            Log.i(TAG, "success connect");
            if (mOnConnectListener != null) {
                mOnConnectListener.onConnect(mRemoteAddress);
            }
            Session session = new Session(socket, swapper, mOnSendListener, mOnReceiveListener, mOnDisconnectCallback, true);
            mSessionSet.add(session);
            session.run();
            return session.getData();

        }
    }

    @Override
    public Future<ReceiveData> startOnNewThread() {
        Objects.requireNonNull(mSwapper, "don't pass Swapper to constructor");

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
        if (mExecutorService != null) {
            mExecutorService.shutdown();
        }
        synchronized (this) {
            for (Session session : mSessionSet) {
                session.disconnect(null);
            }
            mSessionSet.clear();
        }
    }


    @Override
    public ReceiveData call() throws Exception {
        Objects.requireNonNull(mSwapper, "don't pass Swapper to constructor");
        return start(mSwapper);
    }

}
