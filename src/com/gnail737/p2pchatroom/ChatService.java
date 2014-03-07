package com.gnail737.p2pchatroom;

import java.io.IOException;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class ChatService extends Service {

	public static final int SERVER = 0;
	public static final int CLIENT = 1;
	public static final String REQ_TYPE = "com.gnail737.p2pchatroom.REQTYPE";
	public static final String SERVER_TYPE = "Server";
	public static final String CLIENT_TYPE = "Client";
	public static final String TAG = "ChatService";
	
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	HandlerThread mhThread;
	private final IBinder mChatBinder = new ChatBinder();
	
	private ChatServer server = null;
	private ChatClient client = null;
	private Handler mMainHandler;
	private ChatServer.UICallbacks mUICallback;
	private NsdServiceInfo mNsdItem;
	
	public class ChatBinder extends Binder {
		ChatService getService() {
			return ChatService.this;
		}
	}
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		@Override
		public void handleMessage(Message msg) { //startId is stored in msg.arg1
			//start our chat server and client here
			
			if (msg.what == SERVER) {//do server initialization
				if(mMainHandler != null && mUICallback != null) {
					if (server == null) {
						server = new ChatServer(mMainHandler, mUICallback);
						server.init();
					} else if (server.needToInitThread()) {
						server.init();
					}
					
				}
			}
			else if (msg.what == CLIENT) {//do client initialization
				if(mMainHandler != null && mUICallback != null) {
					mNsdItem = (NsdServiceInfo)msg.obj;
					try {
						Socket serverSock = new Socket(mNsdItem.getHost(), mNsdItem.getPort());
						if (client == null) client = new ChatClient(mMainHandler, mUICallback);
						client.init(serverSock);
					} catch(IOException e) {
						Log.e(TAG, "Client Socket Initialization Error!!");
						e.printStackTrace();
					}
				}
			}
		}		
		//do we need to stop after init?
		//stopSelf();
	}
		
	@Override
	public IBinder onBind(Intent i) {
		return mChatBinder;
	}

	@Override
	public void onCreate() {
		//start up thread runnig service
		Log.i(TAG, " here in onCreate method !!!");
		mhThread = new HandlerThread("mServiceHandlerThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
		mhThread.start();
		
		mServiceLooper = mhThread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
//		new Thread( new Runnable() {
//			@Override
//			public void run() {
//				while (mServiceLooper == null) {//block until have valid looper
//					try {
//						Log.i(TAG, "we are stuck until looper initialized!!");
//						Thread.sleep(1000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
				
//			}
//			
//		}).start();
	}
	
	@Override
	public int onStartCommand(Intent in, int flags, int startId) {
		Log.i(TAG, "here we are in start command with startid = "+startId);
		if (in != null) {
			Bundle bundle = in.getExtras();
			if (bundle != null) { //there are chances for NULL intent when process is resurrected after being destroyed by MountainView
				//for each start request send mesages to start job with startId initialized in message body
				if (SERVER_TYPE.equals(bundle.getString(REQ_TYPE))){	
					//start a Server Job
					Message msg = mServiceHandler.obtainMessage(SERVER);
					msg.arg1 = startId;
					mServiceHandler.sendMessage(msg);
					
				} else if (CLIENT_TYPE.equals(bundle.getString(REQ_TYPE))) {
					//start a Client Job
					Message msg = mServiceHandler.obtainMessage(CLIENT);
					msg.arg1 = startId;
					mServiceHandler.sendMessage(msg);
				}
			}
		}
		return START_STICKY;
	}
	
	
	//since we cann't serialize/parcelable interfaces so have to use binder to pass local global var
	//call this only in OnCreate
	public void initAll(Handler mhdl, ChatServer.UICallbacks ucb) {
		mMainHandler = mhdl;
		mUICallback = ucb;
	}
	//to ensure nItem is initialized before ServiceHandler Loop need to send message here 
	public void initAndPostForClient(NsdServiceInfo nItem) {
		mNsdItem = nItem;
		Message msg = mServiceHandler.obtainMessage(CLIENT, (Object)nItem);
		mServiceHandler.sendMessage(msg);
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG," Before service is destroyed!!");
		//TO-DO: adding things to destroy Loopers and all server and client resources
		if(server != null) server.cleanUp();
		if (client != null) client.cleanUp();
		mhThread.quit();
		super.onDestroy();
	}
	
	//following are classes used for communicating with activity
	public void postMessageToServer(final String str) {
		if (server != null)
			server.sendMessages(str);
	}
	
	public void postMessageToClient(final String str) {
		if (client != null)
			client.sendMessages(str);
	}
	//do dummy toast
	public void toastOk() {
		Toast.makeText(this, "Here we are successfully accessing server!!", Toast.LENGTH_LONG).show();
	}
	
	public boolean hasServerOrClient() {
		return (server != null || client != null);
	}
}
