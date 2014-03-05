package com.gnail737.p2pchatroom;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
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

	Handler mainHandler;
	NsdHelper nsdHelper;
	TextView debugView;
	//ListView listView;
	//ChatterRoomAdapter mAdapter;
	
	ChatServer server;
	ChatClient client;
    BlockingQueue<String> sendLines;
    
	String debugMessageCache = "\n";
	public static int PORT = 5134;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
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
				if (server != null) {
					server.sendMessages(et.getText().toString());
				}
				if (client != null) {
					client.sendMessages(et.getText().toString());
					//et.setText("");
				}
				if (server != null || client != null) {
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
				server = new ChatServer(mainHandler, new ChatServer.UICallbacks() {
					@Override
					public void sendMessageToUI(String msg) {
						StringBuilder sb = new StringBuilder(debugMessageCache);
						sb.append("\n"+msg);
						debugMessageCache = sb.toString();
						debugView.setText(debugMessageCache);
					}
				});
				server.init();
			}
			@Override
			public void notifyDiscoveredOneItem(final NsdServiceInfo NsdItem) {
				client = new ChatClient(mainHandler, new ChatServer.UICallbacks() {
					@Override
					public void sendMessageToUI(String msg) {
						StringBuilder sb = new StringBuilder(debugMessageCache);
						sb.append("\n"+msg);
						debugMessageCache = sb.toString();
						debugView.setText(debugMessageCache);
					}
				});
				
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
						    Log.i(TAG, " trying to connect to address" + NsdItem.getHost().toString()+" : "+NsdItem.getPort());
							client.init(new Socket(NsdItem.getHost(), NsdItem.getPort()));
							} catch (IOException e) {
								Log.e(TAG , "Cannot create client Socket!!");
								e.printStackTrace();
							}
						}
					}).start();
				
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
		mainHandler = new Handler();
		nsdHelper = new NsdHelper(this, mainHandler, mListener);
		ChatServer.UICallbacks mCallback = new ChatServer.UICallbacks() {
			@Override
			public void sendMessageToUI(final String msg) {
				StringBuilder sb = new StringBuilder(debugView.getText());
				sb.append(msg);
				debugView.setText(sb.toString());
				sv.post(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						
					}
				});
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
    	super.onDestroy();
    }
    @Override
    protected void onStart() {
    	super.onStart();
    	if (nsdHelper.isRegistered()) {
    		//nsdHelper.stopDiscoverServices();
    		nsdHelper.registerService(PORT);;
    	}
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
