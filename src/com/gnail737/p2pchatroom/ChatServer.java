package com.gnail737.p2pchatroom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class ChatServer {
	protected ReceiverHandler rh;
	protected SenderHandler sh;
	protected final UICallbacks cbs; 
    private final String TAG = "ChatServer";
    Thread mLoopThread = null;
    Socket clientSock = null;
	//use this map to maintain globally available client lists, must be synchronized
    private Map<Integer, ChatNetResourceBundle> mChatClientResources;
	private ServerSocket sSock = null;
    
	public ChatServer(final UICallbacks calls) {
		cbs = calls;
		mChatClientResources = Collections.synchronizedMap(new HashMap<Integer, ChatNetResourceBundle>());
		sh = new SenderHandler(new SenderHandler.HandlerCallbacks() {
			@Override
			public void doneSendingMessage(final String msg) {
				cbs.sendMessageToUI("Me said: "+msg);	
			}
		});
		//After received new client immediately start listening to it by posting new Handler Message
		rh = new ReceiverHandler(new ReceiverHandler.HandlerCallbacks() {
			@Override
			public void hadReceivedNewMessage(final String bundle, final String message) {
				cbs.sendMessageToUI(bundle+" said : "+message);
			}
			@Override
			public void onReceiverReadingError(final int uid) {
				//the pipe is broken, need to remove
				mChatClientResources.get(Integer.valueOf(uid)).cleanUp();
				mChatClientResources.remove(Integer.valueOf(uid));
			}
		});
		sh.start();
		rh.start();
		sh.getLooper();
		rh.getLooper();
		Log.i(TAG, "Server successfully created!!");
	}
	
	protected void init() throws IOException{
		//initiates server socket accepting loop:
		sSock  = new ServerSocket(ChatActivity.PORT);
		
		runLoop(sSock);
		Log.i(TAG, "Server successfully initialized!!");
	}
	@SuppressWarnings("resource")
    void runLoop(final ServerSocket sSock) {
		    if (mLoopThread != null&& mLoopThread.isAlive()) {
		    	try {
					sSock.close();
					//only wait for three seconds until it dies
					mLoopThread.join(3000);
		    	} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}	
		    }
			mLoopThread = new Thread(new Runnable() {
				@Override
				public void run() {
					
					try {
						do {//run loop for server
							clientSock = sSock.accept();
							//after we have a new client socket initialize tcp resource
							final ChatNetResourceBundle cnrb = new ChatNetResourceBundle(clientSock); 
							//adding our key-value pair to hashMap
							mChatClientResources.put(Integer.valueOf(cnrb.getUID()), cnrb);
							if (cbs != null) //there are chances that cbs is null we cannot avoid it
								cbs.sendMessageToUI("New client connected!! ["+clientSock.getInetAddress().toString()+"]");
							//for each client to maintain a place in event loop, the first message must be fired to trigger subsequent messages.
							rh.postNewMessage(cnrb);
						}while (true);
					} catch (IOException e) { //if we have exception we are out!!
							Log.e(TAG, "having exception in accepting Loops now we are out!!!");
							e.printStackTrace();
					} finally{
							//cbs.notifyErrors(1);
					}
				}
			});	
			mLoopThread.start();
	}
	public interface UICallbacks {
		public void sendMessageToUI(final String msg);

		public void notifyErrors(int errCode);
	}
	public void cleanUp() {
		//clean up Server Loop, Receiver, Sender Looper thread
		try {
			if (sSock!=null) sSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//mLoopThread.interrupt();
		sh.cleanUp();
		rh.cleanUp();
		//clean up cache
		Set<Integer> keySet = mChatClientResources.keySet();
		for (Iterator<Integer> i = keySet.iterator(); i.hasNext(); ) {
			mChatClientResources.get(i.next()).cleanUp();
		}
	}
	
	public void sendMessages( final String msg) {
		//Log.i(TAG, "before sending message "+msg+" from "+this.getClass().toString());
		if (sh != null) {
		   //Loop through all available clients and sending message for each one
		  Log.i(TAG, "before sending message "+msg+" from "+this.getClass().toString());
		  Set<Integer> keySet = mChatClientResources.keySet();
		  for (Iterator<Integer> i = keySet.iterator(); i.hasNext(); ) {
			  //here need to create new copy to make sure no synchronization issue
			  ChatNetResourceBundle bundle = ChatNetResourceBundle.clone(mChatClientResources.get(i.next()));
			  bundle.setMessage(msg);
			  sh.postNewMessage(bundle);
		  }
		}
	}
	
	private void debugMessage(final String msg) {
		String className = ChatServer.this.toString();
		cbs.sendMessageToUI("Debug from ["+className.substring(className.indexOf("Chat"))+"]: "+msg);
	}

	public boolean needToInitThread() {
		// TODO Auto-generated method stub
		if (mLoopThread == null) return true;
		else
			return !mLoopThread.isAlive();
	}
}