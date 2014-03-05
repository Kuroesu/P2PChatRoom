package com.gnail737.p2pchatroom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

import android.util.Log;
//this class used to store all shared/cached resources for persistent TCP connection
public class ChatNetResourceBundle {
	private static final String TAG = "ChatNetResourceBundle";
    //UID will be used as HashMap's key
	int UID = 0;

	Socket mSocket = null;

	BufferedReader inStream = null;
	PrintWriter outStream = null;
	//String will only be not null when creating instance to pass to EventLooper
	String message = null;


	boolean isDirty = false;
	
	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}
	
	public Socket getmSocket() {
		return mSocket;
	}
	
	public BufferedReader getInStream() {
		return inStream;
	}

	public void setInStream(BufferedReader inStream) {
		this.inStream = inStream;
	}

	public PrintWriter getOutStream() {
		return outStream;
	}

	public void setOutStream(PrintWriter outStream) {
		this.outStream = outStream;
	}
	
	public void setMessage(String s) {
		message = s;
	}
	
	public String getMessage() {
		return message;
	}
	
	public int getUID() {
		return UID;
	}
	
	//public constructor-- we need to two versions:
	//1) initialize Socket and Streams then cache them 
	public ChatNetResourceBundle(Socket sock) throws IOException {
		mSocket = sock;
		UID = (int) (new Date()).getTime();
		if (!openInStream()) {
			
			throw new IOException();
		}
		
		if (!openOutStream()) {
			
			throw new IOException();
		}
	}
	//2) create new instance of this class to pass to Event Looper at that time we already have socket and streams cached
	public ChatNetResourceBundle(Socket sock, BufferedReader in, PrintWriter out, String msg) {
		mSocket = sock;
		inStream = in;
		outStream = out;
		message = msg;
	}
	//return an exact copy 
	public static ChatNetResourceBundle clone(ChatNetResourceBundle o) {
		return new ChatNetResourceBundle(o.getmSocket(), o.getInStream(), o.getOutStream(), o.getMessage());
	}
	
	//methods to open and cache In/Out NetStream object
	public boolean openInStream() {
		try {
			inStream = new BufferedReader(
					new InputStreamReader(mSocket.getInputStream()));
		} catch (IOException e) {
			Log.e(TAG, "InputStream - failed to open network stream!!");
			e.printStackTrace();
			return false;
		}	
	    return true;
	}
	
	public boolean openOutStream() {
		try {
			outStream = new PrintWriter(new BufferedWriter(
			        new OutputStreamWriter(mSocket.getOutputStream())), true);
		} catch (IOException e) {
			Log.e(TAG, "OutputStream - failed to open network stream!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void cleanUp() {
		
		if (inStream != null) {
			try {
				inStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (outStream != null) {
			outStream.close();
		}
		
		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
	
}
