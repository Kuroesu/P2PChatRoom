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
	public ChatClient(Handler main, UICallbacks calls) {
		super(main, calls);
		// TODO Auto-generated constructor stub
	}
	
    protected void init(final Socket serverSock) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					ChatNetResourceBundle cnrb = null;
					try {
						cnrb = new ChatNetResourceBundle(serverSock);
					} catch (IOException e) {
						Log.e(TAG, "Cannot initialized Client Resource !!");
						e.printStackTrace();
					}
					sh = new SenderHandler(new SenderHandler.HandlerCallbacks() {
						@Override
						public void doneSendingMessage(final String msg) {
							mHandler.post(new Runnable(){
								@Override
								public void run() {
									cbs.sendMessageToUI("Me said: "+msg);
								}});	
						}
					});
					rh = new ReceiverHandler(new ReceiverHandler.HandlerCallbacks() {
						@Override
						public void hadReceivedNewMessage(final String bundle,
								final String msg) {
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									cbs.sendMessageToUI(bundle+" said : "+msg);
								}
							});
						}
						@Override
						public void onReceiverReadingError(int uid) {
							// TODO Error Recovery 
						}
					});
					
					sh.start();
					rh.start();
					sh.getLooper();
					rh.getLooper();
					chatResource = ChatNetResourceBundle.clone(cnrb);
					//push first message for looper
					if (cnrb != null) {
					   rh.postNewMessage(cnrb);
					}
					
				}
			}).start();
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
