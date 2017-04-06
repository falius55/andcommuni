package jp.gr.java_conf.falius.andcommuni;

import android.os.Handler;
import android.os.Looper;

import jp.gr.java_conf.falius.communication.core.Client;

/**
 * Created by ymiyauchi on 2017/04/05.
 *
 * UI操作が可能なOnConnectListenerです。
 */

public abstract class OnUIConnectListener implements Client.OnConnectListener {
    private final Handler mHandler;

    public OnUIConnectListener(Looper looper) {
        mHandler = new Handler(looper);
    }

    @Override
    public final void onConnect(final String s) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                onUIConnect(s);
            }
        };
        mHandler.post(runnable);
    }

    public abstract void onUIConnect(String remoteAddress);
}
