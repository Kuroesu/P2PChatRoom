package com.gnail737.p2pchatroom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;

public class NsdHelper {
	
	Context mContext;
	NsdManager manager;
	NsdManager.DiscoveryListener mDiscoveryListener;
	NsdManager.RegistrationListener mRegistrationListener;
	NsdManager.ResolveListener mResolveListener;
	String pServiceName = "NULL";
	AtomicInteger registrationCount = new AtomicInteger(0);
	//the same IP cannot initiate too many discovery request or it will fail with MAX_LIMIT_REACHED
	AtomicInteger discoverCount = new AtomicInteger(0);
	
	public synchronized String getpServiceName() {
		return pServiceName;
	}

	public synchronized void setpServiceName(String pServiceName) {
		this.pServiceName = pServiceName;
	}

	public static final String SERVICE_TYPE = "_http._tcp.";
	public static final String TAG = "NsdHelper";
	public static final String mServiceName = "P2PChatRoom";
	
	public interface NsdHelperListener {
		public void notifyDiscoveredOneItem(NsdServiceInfo nsdItem);
		public void notifyRegistrationComplete();
		public void outputDebugMessage(final String msg);
	}
	Handler uiHandler;
	List<NsdServiceInfo> mServiceList;
	NsdHelperListener mMainThreadListener;
	
	List<NsdServiceInfo> getmServiceList() {
		//does shallow copy but work for non-synchronized collection
		return new ArrayList<NsdServiceInfo>(mServiceList); 
	}
	//returns whether current machine is registered or not on Nsd
	boolean isRegistered() {
		return (registrationCount.get() > 0);
	}
	//return whether discovery mode is currently on
	boolean isDiscovering() {
		return (discoverCount.get() > 0);
	}
	public NsdHelper(Context ctx, Handler hdl, NsdHelperListener lnr) {
		mContext = ctx;
		uiHandler = hdl;
		mServiceList = Collections.synchronizedList(new ArrayList<NsdServiceInfo>());
		mMainThreadListener = lnr;
		manager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
		initNsd();
	}
	
	public void emptyServiceList() {
		mServiceList.clear();
	}
	
	private void debug(final String msg) {
		mMainThreadListener.outputDebugMessage(msg);
	}
	private void initNsd() {
		initDiscoveryListener();
		initRegistrationListener();
		initResolveListener();
	}

	private void initResolveListener() {
		
		mResolveListener = new NsdManager.ResolveListener() {
			@Override
			public void onServiceResolved(final NsdServiceInfo serviceInfo) {
				// when we resolved a service need to add it to the available list
				Log.d(TAG, "Service Resolution succeed for service = [ "+serviceInfo+" ]");
				debug("Service Resolution succeed for service = [ "+serviceInfo+" ]");
				mServiceList.add(serviceInfo);
				//run on UI Thread
				uiHandler.post(new Runnable() {
					@Override
					public void run() {
						mMainThreadListener.notifyDiscoveredOneItem(serviceInfo);
					}
				});
			}
			
			@Override
			public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
				Log.e(TAG, "onResolveFailed() .. errorCode = "+errorCode);
				debug("onResolveFailed() .. errorCode = "+errorCode);
			}
		};
		
	}

	private void initRegistrationListener() {
		mRegistrationListener = new NsdManager.RegistrationListener() {
			
			@Override
			public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
				Log.e(TAG, "onRegistrationFailed() .. errorCode = "+errorCode);
				debug("onRegistrationFailed() .. errorCode = "+errorCode);
				manager.unregisterService(this);
				
			}
			
			@Override
			public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
				Log.d(TAG, "service name: [ "+serviceInfo.getServiceName()+
						" ] has been unregistered successfully.");
				debug("service name: [ "+serviceInfo.getServiceName()+
						" ] has been unregistered successfully.");
				//decrement registration count 
				registrationCount.decrementAndGet();
				if (pServiceName.equalsIgnoreCase(serviceInfo.getServiceName())) {
					//here need to reset our private ServiceName to signal 
					//Machine is no longer broadcasting itself
					//pServiceName = "NULL";
				}
			}
			
			@Override
			public void onServiceRegistered(NsdServiceInfo serviceInfo) {
				//make a copy of this String so later when we can compare this string to determine
				//if the current machine has been registered to NSD or not
				Log.d(TAG, "entered onServiceRegistered -- \n Registered ServiceInfo = [ "+serviceInfo+" ]");
				debug("entered onServiceRegistered -- \n Registered ServiceInfo = [ "+serviceInfo+" ]");
				pServiceName = String.valueOf(serviceInfo.getServiceName());
				registrationCount.incrementAndGet();
				mMainThreadListener.notifyRegistrationComplete();
			}
			
			@Override
			public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
				Log.e(TAG, "onRegistrationFailed() .. errorCode = "+errorCode);
				debug("onRegistrationFailed() .. errorCode = "+errorCode);
			}
		};
		
	}

	private void initDiscoveryListener() {
		mDiscoveryListener = new NsdManager.DiscoveryListener() {
			
			@Override
			public void onStopDiscoveryFailed(String serviceType, int errorCode) {
				Log.e(TAG, "onStopDiscoveryFailed() .. errorCode = "+errorCode);
				debug("onStopDiscoveryFailed() .. errorCode = "+errorCode);
				//manager.stopServiceDiscovery(this);
				
			}
			
			@Override
			public void onStartDiscoveryFailed(String serviceType, int errorCode) {
				Log.e(TAG, "onStartDiscoveryFailed() .. errorCode = "+errorCode);
				debug("onStartDiscoveryFailed() .. errorCode = "+errorCode);
				//manager.stopServiceDiscovery(this);
				
			}
			
			@Override
			public void onServiceLost(NsdServiceInfo serviceInfo) {
				Log.e(TAG, "Service = [ "+serviceInfo+" ] is now lost..");
				debug("Service = [ "+serviceInfo+" ] is now lost..");
				//find serviceInfo in Array
				for (NsdServiceInfo nServ: mServiceList) {
					if (nServ.equals(serviceInfo)) {
						mServiceList.remove(nServ);
						break;
					}
				}
			}
			
			@Override
			public void onServiceFound(NsdServiceInfo service) {
				Log.d(TAG, "new service found via discovery");
				Log.d(TAG, "Service = [ "+service+" ]");
				debug("on service found: new Service = [ "+service+" ]");
				if (!service.getServiceType().equals(SERVICE_TYPE)) {
					Log.d(TAG, "unknown service type found!!");
					debug("unknown service type found!!");
//              DEBUG  temp relax to allow self loop messaging
				} else if (service.getServiceName().equals(pServiceName)) {
					//arrived here because exact same machine who started Discovery Request found
					Log.d(TAG, "Same machine found!!");
					debug("Same machine found!!");
				} else if (service.getServiceName().contains(mServiceName)) {
					//arrived here when diff machine using the same app is found through Discovery
					Log.d(TAG, "found a new P2P service -[ "+service+" ]..");
					//debug("found a new P2P service -[ "+service+" ]..");
					// only resolve if it is the server
					//if (service.getServiceName().equals(mServiceName))
						manager.resolveService(service, mResolveListener);
				}
				
			}

			@Override
			public void onDiscoveryStopped(String serviceType) {
				Log.d(TAG, "onDiscoveryStopped() called..");
				debug("onDiscoveryStopped() called..");	
				discoverCount.decrementAndGet();
				
			}
			@Override
			public void onDiscoveryStarted(String serviceType) {
				Log.d(TAG, "service discovery started");
				debug("service discovery started");
				discoverCount.incrementAndGet();
			}
		};
		
	}

	public void registerService(int port) {
		Log.d(TAG, "entering registrerServier() on port "+port);
		debug("entering registrerServier() on port "+port);
		NsdServiceInfo serviceInfo = new NsdServiceInfo();
		serviceInfo.setPort(port);
		serviceInfo.setServiceName(mServiceName);
		serviceInfo.setServiceType(SERVICE_TYPE);
		manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
	}
	
	public void discoverServices() {
		Log.d(TAG, "entering discoverServices()");
		debug("entering discoverServices()");
		manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
	}
	
	public void stopDiscoverServices() {
		Log.d(TAG, "entering stopDiscoverServices()");
		debug("entering stopDiscoverServices()");
		if (discoverCount.get()>0)
		   manager.stopServiceDiscovery(mDiscoveryListener);
	}
	
	public void unRegisterService() {
		Log.d(TAG, "entering unregisterService()");
		debug("entering unregisterService()");
		if (registrationCount.get() > 0) {//meaning this service is still registered
			debug("REALLY unregisterService ... ");
			manager.unregisterService(mRegistrationListener);
		}
	}
}
