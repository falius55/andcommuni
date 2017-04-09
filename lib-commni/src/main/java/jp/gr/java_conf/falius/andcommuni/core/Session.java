package jp.gr.java_conf.falius.andcommuni.core;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jp.gr.java_conf.falius.communication.listener.OnDisconnectCallback;
import jp.gr.java_conf.falius.communication.listener.OnReceiveListener;
import jp.gr.java_conf.falius.communication.listener.OnSendListener;
import jp.gr.java_conf.falius.communication.rcvdata.ReceiveData;
import jp.gr.java_conf.falius.communication.senddata.SendData;
import jp.gr.java_conf.falius.communication.swapper.Swapper;

/**
 * Created by ymiyauchi on 2017/04/09.
 */

class Session implements Runnable, AutoCloseable {
    private static final String TAG = Session.class.getName();
    private final BluetoothSocket mSocket;
    private final String mRemoteAddress;
    private final Swapper mSwapper;
    private final OnSendListener mOnSendListener;
    private final OnReceiveListener mOnReceiveListener;
    private final OnDisconnectCallback mOnDisconnectCallback;

    private final InputStream mIn;
    private final OutputStream mOut;
    private BluetoothHandler mNextHandler;


    private boolean mIsContinue = true;

    Session(BluetoothSocket sock, Swapper swapper,
            OnSendListener sendListener, OnReceiveListener onReceiveListener, OnDisconnectCallback onDisconnectCallback)
            throws IOException {
        mSocket = sock;
        mSwapper = swapper;
        mOnSendListener = sendListener;
        mOnReceiveListener = onReceiveListener;
        mOnDisconnectCallback = onDisconnectCallback;
        mIn = sock.getInputStream();
        mOut = sock.getOutputStream();
        mRemoteAddress = sock.getRemoteDevice().getAddress();

        mNextHandler = new BluetoothReadingHandler(this);
    }

    public void run() {
        Log.d(TAG, "session start");
        try {
            while (mIsContinue) {
                BluetoothHandler handler = mNextHandler;
                handler.handle();
            }
        } catch (Throwable e) {
            disconnect(e);
            Log.w(TAG, "handle error, session end ", e);
            return;
        }

        disconnect(null);
        Log.d(TAG, "session end");
    }

    void disconnect(Throwable cause) {
        Log.d(TAG, "session disconnect by " + (cause == null ? "null" : cause.getMessage()));
        mIsContinue = false;
        try {
            mIn.close();
            mOut.close();
            mSocket.close();
        } catch (IOException e) {
            Log.w(TAG, "error during disconnecting", e);
        }
        if (mOnDisconnectCallback != null) {
            mOnDisconnectCallback.onDissconnect(mRemoteAddress, cause);
        }
    }

    void setHandler(BluetoothHandler handler) {
        mNextHandler = handler;
    }

    void onSend() {
        if (mOnSendListener != null) {
            mOnSendListener.onSend(mRemoteAddress);
        }
    }

    void onReceive(ReceiveData receiveData) {
        if (mOnReceiveListener != null) {
            mOnReceiveListener.onReceive(mRemoteAddress, receiveData);
        }
    }

    InputStream getInputStream() throws IOException {
        return mIn;
    }

    OutputStream getOutputStream() throws IOException {
        return mOut;
    }

    SendData newSendData(ReceiveData latestReceiveData) throws Exception {
        try {
            return mSwapper.swap(mRemoteAddress, latestReceiveData);
        } catch (Exception e) {
            throw new Exception("thrown exception from swap method");
        }
    }

    boolean doContinue() {
        return mSwapper.doContinue();
    }

    @Override
    public String toString() {
        return mSocket.toString();
    }

    @Override
    public void close() throws IOException {
        disconnect(null);
    }

}
