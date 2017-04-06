package jp.gr.java_conf.falius.andcommuni;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import jp.gr.java_conf.falius.communication.header.Header;
import jp.gr.java_conf.falius.communication.header.HeaderFactory;
import jp.gr.java_conf.falius.communication.listener.OnSendListener;
import jp.gr.java_conf.falius.communication.senddata.SendData;

/**
 * Created by ymiyauchi on 2017/04/06.
 */

final class WritingHandler {
    private final String mRemoteAddress;
    private final OutputStream mOut;
    private final SendData mSendData;
    private final OnSendListener mListener;

    WritingHandler(String remoteAddress, OutputStream out, SendData sendData, OnSendListener listener) {
        mRemoteAddress = remoteAddress;
        mOut = out;
        mSendData = sendData;
        mListener = listener;
    }

    void send() throws IOException {
        Header header = HeaderFactory.from(mSendData);
        byte[] headerBytes = header.toByteBuffer().array();
        mOut.write(headerBytes);
        for (ByteBuffer dataBuf : mSendData) {
            byte[] dataBytes = dataBuf.array();
            mOut.write(dataBytes);
        }
        mOut.flush();
        if (mListener != null) {
            mListener.onSend(mRemoteAddress);
        }
    }
}
