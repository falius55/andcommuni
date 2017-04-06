package jp.gr.java_conf.falius.andcommuni;

import android.os.Handler;
import android.os.Looper;

import jp.gr.java_conf.falius.communication.listener.OnReceiveListener;
import jp.gr.java_conf.falius.communication.rcvdata.ReceiveData;

/**
 * Created by ymiyauchi on 2017/04/05.
 *
 * UI操作が可能なOnReceiveListenerです。
 */

public abstract class OnUIReceiveListener implements OnReceiveListener {
    private final Handler mHandler;

    public OnUIReceiveListener(Looper looper) {
        mHandler = new Handler(looper);
    }

    @Override
    public final void onReceive(final String remoteAddress, final ReceiveData receiveData) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                onUIReceive(remoteAddress, receiveData);
            }
        };
        mHandler.post(runnable);
    }

    public abstract void onUIReceive(String remoteAddress, ReceiveData receiveData);
}
