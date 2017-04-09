package jp.gr.java_conf.falius.andcommuni.core;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

import jp.gr.java_conf.falius.communication.header.Header;
import jp.gr.java_conf.falius.communication.header.HeaderFactory;
import jp.gr.java_conf.falius.communication.rcvdata.BasicReceiveData;
import jp.gr.java_conf.falius.communication.rcvdata.ReceiveData;
import jp.gr.java_conf.falius.communication.senddata.SendData;

class BluetoothReadingHandler implements BluetoothHandler {
    private static final String TAG = BluetoothReadingHandler.class.getName();
    private final Session mSession;

    BluetoothReadingHandler(Session session) {
        mSession = session;
    }

    public void handle() throws Exception {
        Log.d(TAG, "reading handle");
        InputStream is = mSession.getInputStream();
        Header header = HeaderFactory.from(is);
        if (header == null) {
            mSession.disconnect(null);
            return;
        }
        Entry entry = new Entry(header);
        int readBytes = entry.read(is);
        if (readBytes < 0) {
            mSession.disconnect(null);
            return;
        }
        ReceiveData data = entry.getData();

        mSession.onReceive(data);
        mSession.setData(data);

        if (!mSession.isClient() || mSession.doContinue()) {
            SendData sendData = mSession.newSendData(data);
            BluetoothHandler handler = new BluetoothWritingHandler(mSession, sendData);
            mSession.setHandler(handler);
        } else {
            mSession.disconnect(null);
        }

    }

    /**
     * 一度の受信単位
     * @author "ymiyauchi"
     *
     */
    private static class Entry {
        private Header mHeader;
        private int mRemain;
        private Queue<ByteBuffer> mItemData = null;

        private Entry(Header header) {
            mHeader = header;
            mRemain = mHeader.allDataSize() - mHeader.size();
            Log.d(TAG, "all data size: " + mHeader.allDataSize());
            initItemData();
        }

        private void initItemData() {
            mItemData = new ArrayDeque<>();
            IntBuffer sizeBuf = mHeader.dataSizeBuffer();
            Log.d(TAG, "data size buffer : " + sizeBuf);
            while (sizeBuf.hasRemaining()) {
                int size = sizeBuf.get();
                Log.d(TAG, "item buf size: " + size);
                ByteBuffer buf = ByteBuffer.allocate(size);
                mItemData.add(buf);
            }
        }

        private int read(InputStream is) throws IOException {
            Log.d(TAG, "entry read");
            int readed = 0;
            for (ByteBuffer itemBuf : mItemData) {
                byte[] bytes = new byte[itemBuf.limit()];
                int tmp = is.read(bytes);
                if (tmp < 0) {
                    Log.d(TAG, "read error -1 return");
                    return -1;
                }
                Log.d(TAG, "read bytes: " + tmp);
                itemBuf.put(bytes);
                readed += tmp;
            }
            mRemain -= readed;
            Log.d(TAG, "entry readed : mRemain is " + mRemain);
            return readed;
        }

        private ReceiveData getData() {
            if (!isFinished()) {
                return null;
            }
            for (ByteBuffer data : mItemData) {
                data.flip();
            }
            return new BasicReceiveData(mItemData);
        }

        private boolean isFinished() {
            return mHeader.isReadFinished() && mRemain == 0;
        }
    }
}
