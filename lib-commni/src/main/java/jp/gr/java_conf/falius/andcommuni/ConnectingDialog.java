package jp.gr.java_conf.falius.andcommuni;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * Created by ymiyauchi on 2017/04/05.
 */

public class ConnectingDialog extends DialogFragment {
    private static final String MESSAGE = "Bluetoothを接続しています...";
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(MESSAGE);
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        return dialog;
    }
}
