package jp.gr.java_conf.falius.andcommuni.uilistener;

import android.os.Handler;
import android.os.Looper;

import jp.gr.java_conf.falius.communication.core.Server;

/**
 * Created by ymiyauchi on 2017/04/07.
 */

public abstract class OnUIAcceptListener implements Server.OnAcceptListener {
    private final Handler mHandler;

    public OnUIAcceptListener(Looper looper) {
        mHandler = new Handler(looper);
    }

    @Override
    public final void onAccept(final String remoteAddress) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                onUIAccept(remoteAddress);
            }
        };
        mHandler.post(r);
    }

    protected abstract void onUIAccept(String remoteAddress);
}
