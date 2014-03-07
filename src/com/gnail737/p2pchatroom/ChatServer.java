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
    protected Handler mHandler = null;
	protected final UICallbacks cbs; 
    private final String TAG = "ChatServer";
    Thread mLoopThread = null;
	//use this map to maintain globally available client lists, must be synchronized
    private Map<Integer, ChatNetResourceBundle> mChatClientResources;
    
	public ChatServer(Handler main, final UICallbacks calls) {
		mHandler = main;
		cbs = calls;
		mChatClientResources = Collections.synchronizedMap(new HashMap<Integer, ChatNetResourceBundle>());
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
		//After received new client immediately start listening to it by posting new Handler Message
		rh = new ReceiverHandler(new ReceiverHandler.HandlerCallbacks() {
			@Override
			public void hadReceivedNewMessage(final String bundle, final String message) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						cbs.sendMessageToUI(bundle+" said : "+message);
					}
				});
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
	}
	
	protected void init() {
		final ServerSocket sSock;
		//initiates server socket accepting loop:
		try {
			sSock = new ServerSocket(ChatActivity.PORT);
		} catch (IOException e) {
			Log.e(TAG , "ServerSocket init failed !!!");
			e.printStackTrace();
			return;
		}
		
		runLoop(sSock);
	}
	@SuppressWarnings("resource")
    void runLoop(final ServerSocket sSock) {
		    if (mLoopThread != null) {
		    	mLoopThread.interrupt();
		    }
			mLoopThread = new Thread(new Runnable() {
				@Override
				public void run() {
					do {//run loop for server
						try {
							final Socket clientSock = sSock.accept();
							//after we have a new client socket initialize tcp resource
							final ChatNetResourceBundle cnrb = new ChatNetResourceBundle(clientSock); 
							//adding our key-value pair to hashMap
							mChatClientResources.put(Integer.valueOf(cnrb.getUID()), cnrb);
							mHandler.post(new Runnable(){
								@Override
								public void run() {
									cbs.sendMessageToUI("New client connected!! ["+clientSock.getInetAddress().toString()+"]");
							}});
							//for each client to maintain a place in event loop, the first message must be fired to trigger subsequent messages.
							rh.postNewMessage(cnrb);
							Thread.sleep(2000);
						} catch (IOException e) {
							Log.e(TAG, "in Server Accepting new Connections Errors!!!");
							e.printStackTrace();
						} catch (InterruptedException e) {
							Log.e(TAG, "we are interrupted!!!");
							e.printStackTrace();
							return;
						} finally{
							//TO-DO: may need to do something about releasing resource
						}
					}while (true);
				}
			});	
			mLoopThread.start();
	}
	
	public interface UICallbacks {
		public void sendMessageToUI(final String msg);
	}
	
	public void cleanUp() {
		//clean up Server Loop, Receiver, Sender Looper thread
		mLoopThread.interrupt();
		sh.cleanUp();
		rh.cleanUp();
		//clean up cache
		Set<Integer> keySet = mChatClientResources.keySet();
		for (Iterator<Integer> i = keySet.iterator(); i.hasNext(); ) {
			mChatClientResources.get(i.next()).cleanUp();
		}
//		try {
//			mLoopThread.join(2000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
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
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				String className = ChatServer.this.toString();
				cbs.sendMessageToUI("Debug from ["+className.substring(className.indexOf("Chat"))+"]: "+msg);
			}
		});
	}

	public boolean needToInitThread() {
		// TODO Auto-generated method stub
		if (mLoopThread == null) return true;
		else
			return !mLoopThread.isAlive();
	}
}