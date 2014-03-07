package com.gnail737.p2pchatroom;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class SenderHandler extends HandlerThread {
    private final static String TAG = "SenderThread";
    private final static int SEND = 0;
    static Handler mHandler = null;
    PrintWriter netOutput = null;
	HandlerCallbacks handlerCallback = null;
    
    //main worker method
    public void postNewMessage(final ChatNetResourceBundle msg) {
    	mHandler.obtainMessage(SEND, msg).sendToTarget();
    }

//	public synchronized void setmSocket(Socket mSock) {
//		if (mSocket != null) { //we previously had a connection do cleanup first
//			try {
//				//mark outputstream also
//				netOutput.close();
//				netOutput = null;
//				mSocket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			if (mHandler != null) {
//				mHandler.removeMessages(SEND);
//			}
//		}
//		this.mSocket = mSock;
//	}
	
	public interface HandlerCallbacks {
		void doneSendingMessage(final String msg);
	}
	
	public SenderHandler(HandlerCallbacks hcb) {
		super(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
		handlerCallback = hcb;
	}
	@Override
	protected void onLooperPrepared() {
		mHandler = new Handler() {
		    @Override
		    public void handleMessage(Message msg) {
		    	if (msg.what == SEND) {
		    		Log.i(TAG, "Before handling SEND event");
		    		final ChatNetResourceBundle message = (ChatNetResourceBundle)msg.obj;
		    		handleSendEvent(message);
		    	}
		    }

			private void handleSendEvent(ChatNetResourceBundle msg) {
				msg.getOutStream().println(msg.getMessage());
				handlerCallback.doneSendingMessage(msg.getMessage()+" to "+msg.mSocket.toString());
			}
	    };
	}
	
	public void cleanUp() {
		if (mHandler != null) {
			mHandler.removeMessages(SEND);
		}
		this.quit();
	}
	    
}
