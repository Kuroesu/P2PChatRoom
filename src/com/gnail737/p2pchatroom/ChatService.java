package com.gnail737.p2pchatroom;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class ChatService extends Service {

	public static final int SERVER = 0;
	public static final int CLIENT = 1;
	public static final String REQ_TYPE = "REQTYPE";
	public static final String SERVER_TYPE = "Server";
	public static final String CLIENT_TYPE = "Client";
	public static final String TAG = "ChatService";
	
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private final IBinder mChatBinder = new ChatBinder();
	
	ChatServer server;
	ChatClient client;
	
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

			}
			else if (msg.what == CLIENT) {//do client initialization
				
			}
			
			//do we need to stop after init?
			//stopSelf();
		}
		
	}
	@Override
	public IBinder onBind(Intent i) {
		return mChatBinder;
	}

	@Override
	public void onCreate() {
		//start up thread runnig service
		HandlerThread mhThread = new HandlerThread("mServiceHandlerThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
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
		Bundle bundle = in.getExtras();
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
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		
	}
	
	//following are classes used for communicating with activity
	public void postCreateNewServer( ) {
		
	}
	
	public void postCreateNewClient() {
		
	}
}
