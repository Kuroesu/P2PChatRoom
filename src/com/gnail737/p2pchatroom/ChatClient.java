package com.gnail737.p2pchatroom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;

import android.os.Handler;
import android.util.Log;

public class ChatClient extends ChatServer{

	private final String TAG = "ChatClient";
	ChatNetResourceBundle chatResource = null;
	public ChatClient(UICallbacks calls) {
		
		super(calls);
		sh = new SenderHandler(new SenderHandler.HandlerCallbacks() {
			@Override
			public void doneSendingMessage(final String msg) {
				cbs.sendMessageToUI("Me said: "+msg);
			}
		});
		rh = new ReceiverHandler(new ReceiverHandler.HandlerCallbacks() {
			@Override
			public void hadReceivedNewMessage(final String bundle,
					final String msg) {
				cbs.sendMessageToUI(bundle+" said : "+msg);
			}
			@Override
			public void onReceiverReadingError(int uid) {
				chatResource.cleanUp();
				chatResource = null;
			}
		});
		//get Looper ready
		sh.start();
		rh.start();
		sh.getLooper();
		rh.getLooper();
	}
	
    protected void init(final Socket serverSock) throws IOException {
		ChatNetResourceBundle cnrb = null;
		cnrb = new ChatNetResourceBundle(serverSock);
		chatResource = ChatNetResourceBundle.clone(cnrb);
		//push first message for receiving looper 
		if (cnrb != null) {
			rh.postNewMessage(cnrb);
		}	
    }
    
    @Override
	public void sendMessages( final String msg) {
		//Log.i(TAG, "before sending message "+msg+" from "+this.getClass().toString());
		if (sh != null && chatResource != null) {
		   //Loop through all available clients and sending message for each one
		  Log.i(TAG, "before sending message "+msg+" from "+this.getClass().toString());
			  ChatNetResourceBundle bundle = ChatNetResourceBundle.clone(chatResource);
			  bundle.setMessage(msg);
			  sh.postNewMessage(bundle);
		  }
	}
    @Override
    public void cleanUp() {
    	if (chatResource != null) {
    		chatResource.cleanUp();
			chatResource = null;
    	}
    	//just need to exit looper 
    	sh.cleanUp();
		rh.cleanUp();
    }
//	Socket mSocket;
//	public synchronized Socket getmSocket() {
//		return mSocket;
//	}
//
//	public synchronized void setmSocket(Socket mSocket) {
//		this.mSocket = mSocket;
//	}

//	public ChatClient(UICallbacks cbs, Socket sock) {
//		super(cbs);
//		mSocket = sock;
//	}
	
//	@Override
//	public void run() { //unlike server, client only need to run once. coz sender and receiving is doing loop job
//		//before call run make sure have a socket to communicate with
//		if (mSocket == null) return;
//		
//		mClientSocket = mSocket;
//		prepareSendReceiveThreads();
//		
//	}
}
