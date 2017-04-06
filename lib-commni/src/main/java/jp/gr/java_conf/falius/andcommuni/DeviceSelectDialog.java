package jp.gr.java_conf.falius.andcommuni;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import jp.gr.java_conf.falius.util.range.IntRange;

/**
 * Created by ymiyauchi on 2017/04/05.
 *
 * <p>
 * ペアリング済のデバイスからひとつのデバイスを選択するダイアログを表示します。
 * 選択するとこのダイアログを表示したアクティビティあるいはフラグメントに実装された
 *     {@link }DeviceSelector}のonSelectメソッドの引数に選択したデバイスが渡されます。
 *
 * <p>
 *     アクティビティあるいはフラグメントはDeviceSelectorインターフェースを実装し、onResumeメソッドにて
 *     ダイアログを作成、onSelectメソッドで渡されたBluetoothDeviceを用いて{@link }BluetoothClient}を
 *     インスタンス化してフィールドに保存、onDestroyメソッドにてBluetoothClientをcloseするという流れ
 *     を想定しています。
 * </p>
 */

public class DeviceSelectDialog extends DialogFragment {
    private static int REQUEST_ENABLE_BLUETOOTH = 14;
    private DeviceSelector mListener = null;

    /**
     * アクティビティから呼び出される際に利用されるファクトリメソッドです。
     * @return
     */
    public static DeviceSelectDialog newInstance() {
        return new DeviceSelectDialog();
    }

    /**
     * フラグメントから呼び出される際に利用されるファクトリメソッドです。
     * @param fragment このダイアログを呼び出すフラグメント
     * @return
     */
    public static DeviceSelectDialog newInstance(Fragment fragment) {
        DeviceSelectDialog dialog = newInstance();
        dialog.setTargetFragment(fragment, 0);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment fragment = getTargetFragment();
        try {
            if (fragment == null) {
                mListener = (DeviceSelector) context;
            } else {
                mListener = (DeviceSelector) fragment;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException("Don't implements DeviceSelector");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new IllegalStateException("not found bluetooth device");
        }
        if (!adapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
        }

        final BluetoothDevice[] devices = adapter.getBondedDevices().toArray(new BluetoothDevice[0]);
        String[] deviceNames = new String[devices.length];
        for (int i : new IntRange(devices.length)) {
            deviceNames[i] = devices[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder.setTitle("select device")
                .setItems(deviceNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.onSelect(devices[i]);
                        dismiss();
                    }
                }).create();
    }

    /**
     * Bluetoothをオンにするよう要求した後に呼び出されるメソッドです。
     * @param requestCode
     * @param resultCode
     * @param data
     * @throws IllegalStateException Bluetoothをオンにするよう要求したにもかかわらず拒まれた場合
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                // ignore
            } else {
                throw new IllegalStateException("bluetooth is not ON");
            }
        }
    }
}
