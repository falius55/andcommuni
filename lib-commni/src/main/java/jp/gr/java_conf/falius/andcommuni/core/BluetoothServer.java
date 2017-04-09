package jp.gr.java_conf.falius.andcommuni.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jp.gr.java_conf.falius.communication.core.Server;
import jp.gr.java_conf.falius.communication.listener.OnDisconnectCallback;
import jp.gr.java_conf.falius.communication.listener.OnReceiveListener;
import jp.gr.java_conf.falius.communication.listener.OnSendListener;
import jp.gr.java_conf.falius.communication.swapper.SwapperFactory;

/**
 * Created by ymiyauchi on 2017/04/09.
 */

public class BluetoothServer implements Server {
    private static final String TAG = BluetoothServer.class.getName();

    private final SwapperFactory mSwapperFactory;
    private final BluetoothServerSocket mServerSocket;
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    private OnSendListener mOnSendListener = null;
    private OnReceiveListener mOnReceiveListener = null;
    private OnDisconnectCallback mOnDisconnectCallback = null;
    private Server.OnAcceptListener mOnAcceptListener = null;
    private Server.OnShutdownCallback mOnShutdownCallback = null;

    private volatile boolean mIsShutdowned = false;

    public BluetoothServer(String uuid, String serviceName, SwapperFactory swapperFactory) throws IOException {
        mSwapperFactory = swapperFactory;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new IllegalStateException("this device does not support bluetooth");
        }
        mServerSocket = adapter.listenUsingRfcommWithServiceRecord(serviceName, UUID.fromString(uuid));
    }

    private void exec() throws IOException {
        try {
            while (!mIsShutdowned) {
                BluetoothSocket sock = mServerSocket.accept();
                Session session = new Session(sock, mSwapperFactory.get(),
                        mOnSendListener, mOnReceiveListener, mOnDisconnectCallback);
                mExecutor.submit(session);
                if (mOnAcceptListener != null) {
                    String remoteAddress = sock.getRemoteDevice().getAddress();
                    mOnAcceptListener.onAccept(remoteAddress);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "exec error", e);
            throw new IOException("execution error", e);
        }
    }

    @Override
    public void addOnReceiveListener(OnReceiveListener onReceiveListener) {
        mOnReceiveListener = onReceiveListener;
    }

    @Override
    public void addOnSendListener(OnSendListener onSendListener) {
        mOnSendListener = onSendListener;
    }

    @Override
    public void addOnAcceptListener(Server.OnAcceptListener onAcceptListener) {
        mOnAcceptListener = onAcceptListener;
    }

    @Override
    public void addOnShutdownCallback(OnShutdownCallback onShutdownCallback) {
        mOnShutdownCallback = onShutdownCallback;
    }

    @Override
    public Future<?> startOnNewThread() {
        return mExecutor.submit(this);
    }

    @Override
    public void close() throws IOException {
        shutdown();
    }

    @Override
    public void shutdown() throws IOException {
        if (mServerSocket != null) {
            mServerSocket.close();
        }
        mExecutor.shutdown();
        mIsShutdowned = true;

        if (mOnShutdownCallback != null) {
            mOnShutdownCallback.onShutdown();
        }

    }

    @Override
    public void addOnDisconnectCallback(OnDisconnectCallback onDisconnectCallback) {
        mOnDisconnectCallback = onDisconnectCallback;
    }

    @Override
    public Throwable call() throws Exception {
        exec();
        return null;
    }
}
