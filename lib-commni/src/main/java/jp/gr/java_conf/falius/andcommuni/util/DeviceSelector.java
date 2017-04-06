package jp.gr.java_conf.falius.andcommuni.util;

import android.bluetooth.BluetoothDevice;

/**
 * Created by ymiyauchi on 2017/04/05.
 */

public interface DeviceSelector {

    void onSelect(BluetoothDevice device);
}
