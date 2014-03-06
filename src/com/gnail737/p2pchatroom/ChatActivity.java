package com.gnail737.p2pchatroom;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class ChatActivity extends Activity {
	private final String TAG = "ChatActiivty";
	Handler mainHandler;
	ChatServer.UICallbacks mCallbacks;
	NsdHelper nsdHelper;
	TextView debugView;
	//ListView listView;
	//ChatterRoomAdapter mAdapter;
    
	private ChatService mBoundService;
	private boolean mIsBound = false;
	private ServiceConnection mConnection = new ServiceConnection() {


		public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundService = ((ChatService.ChatBinder)service).getService();
			Log.i(TAG, "Service is bounded!!!");
			mBoundService.toastOk();
			mBoundService.initAll(mainHandler, mCallbacks);
		}
		
		public void onServiceDisconnected(ComponentName className) {
			mBoundService = null;
		}
	};
	
	private void doBindService() {
		bindService(new Intent(this, ChatService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}
	
	private void doUnbindService() {
		if (mIsBound){
			unbindService(mConnection);
			mIsBound = false;
		}
	}
	
	private void startChatRoomService(String type) {
		Intent i = new Intent(this, ChatService.class);
		Bundle extra = new Bundle();
		extra.putString(ChatService.REQ_TYPE, type);
		i.putExtras(extra);
		startService(i);
	}
	
	String debugMessageCache = "\n";
	public static int PORT = 5134;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
		mainHandler = new Handler();
        //setuping up views
        Switch registerSth = (Switch)findViewById(R.id.registerSwitch);
        Switch discoverSth = (Switch)findViewById(R.id.discoverySwitch);
        final ScrollView sv = (ScrollView) findViewById(R.id.scrollBar);
        //registerSth.setChecked(true);
        debugView = (TextView)findViewById(R.id.debugMessage);
        //black background white text int is ARGB format
        debugView.setBackgroundColor(0xff101010);
        debugView.setTextColor(0xfff9f9f9);
        final EditText et = (EditText) findViewById(R.id.editText);
        Button sendBtn = (Button) findViewById(R.id.sendMessage);
        
        sendBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//debuging code
				//mBoundService.toastOk();
				//Null check is done inside Service Object
				
			    mBoundService.postMessageToServer(et.getText().toString());
				mBoundService.postMessageToClient(et.getText().toString());
				
				if (mBoundService.hasServerOrClient()) {
					et.setText("");
				}
			}
		});
        registerSth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {//user change it to checked status
					if (!nsdHelper.isRegistered()) //only register it when not already registered
						   nsdHelper.registerService(PORT);
				}else {
					if (nsdHelper.isRegistered())
						nsdHelper.unRegisterService();
				}
			}
		});
        
        discoverSth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					if (!nsdHelper.isDiscovering())
					nsdHelper.discoverServices();
				}else {
					if (nsdHelper.isDiscovering())
					nsdHelper.stopDiscoverServices();
				}
				
			}
		});
        //seting up NsdHelper
        NsdHelper.NsdHelperListener mListener = new NsdHelper.NsdHelperListener() {			
			private String TAG = "ChatActivity";
			@Override
			public void notifyRegistrationComplete() {
				//now we are ready to start a server
				startChatRoomService(ChatService.SERVER_TYPE);
			}
			@Override
			public void notifyDiscoveredOneItem(final NsdServiceInfo NsdItem) {
				Log.i(TAG, " trying to connect to address" + NsdItem.getHost().toString()+" : "+NsdItem.getPort());
			    mBoundService.initAndPostForClient(NsdItem);
			    //don't need to use startCommand to launch client because the timing of NsdItem initialization is uncertain 
				//startChatRoomService(ChatService.CLIENT_TYPE);
				
//				List<NsdServiceInfo> mList = nsdHelper.getmServiceList();
//				mAdapter.setChatterList(mList);
//				mAdapter.notifyDataSetChanged();
//				listView.postInvalidate();
			}
			@Override
			public void outputDebugMessage(final String msg) {
				StringBuilder sb = new StringBuilder(debugMessageCache);
				sb.append("\n"+msg);
				debugMessageCache = sb.toString();
				debugView.setText(debugMessageCache);
				
			}
		};

		nsdHelper = new NsdHelper(this, mainHandler, mListener);
		
		mCallbacks = new ChatServer.UICallbacks() {
			@Override
			public void sendMessageToUI(String msg) {
				StringBuilder sb = new StringBuilder(debugMessageCache);
				sb.append("\n"+msg);
				debugMessageCache = sb.toString();
				debugView.setText(debugMessageCache);
			}
		};
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }
    
    @Override
    protected void onDestroy() {
    	//clean up our Nsd Service upon exit
    	doUnbindService();
    	super.onDestroy();
    }
    @Override
    protected void onStart() {
    	super.onStart();
    	if (nsdHelper.isRegistered()) {
    		//nsdHelper.stopDiscoverServices();
    		nsdHelper.registerService(PORT);;
    	}
    	doBindService();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    }
    @Override
    protected void onPause() {
    	//stop discovering
    	//nsdHelper.stopDiscoverServices();
    	super.onPause();
    }
    @Override
    protected void onStop() {
    	if (nsdHelper.isDiscovering()) {
    		nsdHelper.stopDiscoverServices();
    	}
    	if (nsdHelper.isRegistered()) {
    		nsdHelper.unRegisterService();
    	}
    	super.onStop();
    }
}
