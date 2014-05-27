package com.gnail737.p2pchatroom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.gnail737.p2pchatroom.ChatServer.UICallbacks;

import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;

public class ChatClient extends ChatServer{
	/* following are inherited members from ChatServer
	protected ReceiverHandler rh;
	protected SenderHandler sh;
	protected final UICallbacks cbs;
	protected Map<String, ChatNetResourceBundle> mChatClientResources;
	**************************************************/
	private final String TAG = "ChatClient";
	ChatNetResourceBundle chatResource = null;
	
	public ChatNetResourceBundle getChatResource() {
		return chatResource;
	}
	public ChatClient(UICallbacks calls) {
		super(calls);
		
	}
	@Override
	protected void initLoopers() {
		if (sh != null &&sh.isAlive()) return;
		if (rh != null &&rh.isAlive()) return;
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
			public void onReceiverReadingError(String uid) {
				if (mChatClientResources.containsKey(uid)) {
					mChatClientResources.get(uid).cleanUp();
					mChatClientResources.remove(uid);
					//cbs.notifyErrors(2);
				}
				
			}
		});
		//get Looper ready
		sh.start();
		rh.start();
		sh.getLooper();
		rh.getLooper();
		Log.i(TAG, "Client successfully created!!");	
	}
	
    protected void init(final NsdServiceInfo nsdItem) throws IOException {
    	//check to see if we already connected to the same server
		if (mChatClientResources.containsKey(nsdItem.getHost().getHostAddress())) {
			return;
		}
		ChatNetResourceBundle cnrb;
		Socket serverSock = new Socket(nsdItem.getHost(), nsdItem.getPort());
		cnrb = new ChatNetResourceBundle(serverSock);
		//chatResource = ChatNetResourceBundle.clone(cnrb);
		//add new chat resource to hash map
		mChatClientResources.put(new String(cnrb.getUID()), cnrb);
		//push first message for receiving looper 
		if (cnrb != null) {
			rh.postNewMessage(cnrb.clone(cnrb));
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
//    @Override
//    public void cleanUp() {
//    	if (chatResource != null) {
//    		chatResource.cleanUp();
//			chatResource = null;
//    	}
//    	//just need to exit looper 
//    	sh.cleanUp();
//		rh.cleanUp();
//    }
}
