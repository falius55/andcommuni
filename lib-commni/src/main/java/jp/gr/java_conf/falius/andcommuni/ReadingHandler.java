package jp.gr.java_conf.falius.andcommuni;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

import jp.gr.java_conf.falius.communication.header.Header;
import jp.gr.java_conf.falius.communication.header.HeaderFactory;
import jp.gr.java_conf.falius.communication.listener.OnReceiveListener;
import jp.gr.java_conf.falius.communication.rcvdata.BasicReceiveData;
import jp.gr.java_conf.falius.communication.rcvdata.ReceiveData;

/**
 * Created by ymiyauchi on 2017/04/06.
 */

final class ReadingHandler {
    private final String mRemoteAddress;
    private final InputStream mIn;
    private final OnReceiveListener mListener;

    ReadingHandler(String remoteAddress, InputStream in, OnReceiveListener listener) {
        mRemoteAddress = remoteAddress;
        mIn = in;
        mListener = listener;
    }

    @Nullable
    ReceiveData receive() throws IOException {
        Header header = HeaderFactory.from(mIn);
        Entry entry = new Entry(header);
        int readBytes = entry.read(mIn);
        if (readBytes < 0) {
            return null;
        }
        ReceiveData receiveData = entry.getData();

        if (mListener != null) {
            mListener.onReceive(mRemoteAddress, receiveData);
        }
        return receiveData;
    }

    /**
     * 一度の受信単位
     * @author "ymiyauchi"
     *
     */
    private static class Entry {
        private final Header mHeader;
        private final Queue<ByteBuffer> mItemData = new ArrayDeque<>();
        private int mRemain;

        private Entry(Header header) {
            mHeader = header;
            mRemain = mHeader.allDataSize() - mHeader.size();
            initItemData(header);
        }

        private void initItemData(Header header) {
            IntBuffer sizeBuf = header.dataSizeBuffer();
            while (sizeBuf.hasRemaining()) {
                int size = sizeBuf.get();
                ByteBuffer buf = ByteBuffer.allocate(size);
                mItemData.add(buf);
            }
        }

        private int read(InputStream is) throws IOException {
            int readBytes = 0;
            for (ByteBuffer itemBuf : mItemData) {
                byte[] bytes = new byte[itemBuf.limit()];
                int tmp = is.read(bytes);
                if (tmp < 0) {
                    return -1;
                }
                itemBuf.put(bytes);
                readBytes += tmp;
            }
            mRemain -= readBytes;
            return readBytes;
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
