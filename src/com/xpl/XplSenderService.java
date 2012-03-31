package com.xpl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


public class XplSenderService extends Service {
	public XplSenderService() {
		super();
		//Log.d(TAG,"constructor");
		//this.getApplicationContext().registerReceiver(this, new IntentFilter(XplSenderService.class.toString()));
	}

	private static String TAG = "com.xpl";
	public static String EXTRA_XPLMESSAGE="com.xpl.xplmessage";
    
    static int xpl_port = 3865;
    
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	
        	
        	String toSend = msg.getData().getString("message");
        	//Log.d(TAG,"thread handle: " + toSend);
            sendMessageNet(toSend);
            
            //Log.d(TAG,"thread handle done");
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
    	//Log.d(TAG,"serv onCreate");
      // Start up the thread running the service.  Note that we create a
      // separate thread because the service normally runs in the process's
      // main thread, which we don't want to block.  We also make it
      // background priority so CPU-intensive work will not disrupt our UI.
      HandlerThread thread = new HandlerThread("ServiceStartArguments");
      thread.start();
      
      // Get the HandlerThread's Looper and use it for our Handler 
      mServiceLooper = thread.getLooper();
      mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	//Log.d(TAG,"serv startCommand");
    	
    	String message = "none?";
        Bundle extras = intent.getExtras();
        if (extras != null) {
        	message = extras.getString(EXTRA_XPLMESSAGE);
        } else {
        	Log.d(TAG,"no extras!");
        }
    	
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        Bundle b = new Bundle();
        b.putString("message", message);
        msg.setData(b);
        mServiceHandler.sendMessage(msg);
        
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }
    
    @Override
    public void onDestroy() {
    	//Log.d(TAG,"serv destryoed");
      //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show(); 
    }

	
	 private InetAddress getBcastAddr() {
		 	Context context = this.getApplicationContext();
	    	ConnectivityManager connman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    	Boolean isWifi = connman.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting(); 
	    	
	    	WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	    	if (!isWifi) {
	    		//Log.d(TAG,"looks like we're not on wifi, bailing");
	    		Toast.makeText(this, "Can not send: not on Wifi?", Toast.LENGTH_SHORT).show(); 
	    		return null;
	    	}
	    	
	        DhcpInfo dhcp = wifi.getDhcpInfo();
	        // handle null somehow

	        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
	        byte[] quads = new byte[4];
	        for (int k = 0; k < 4; k++)
	          quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
	        try {
	        	InetAddress addr = InetAddress.getByAddress(quads);
				return addr;
			} catch (UnknownHostException e) {
				Log.e(TAG,"Error getting address:");
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}

	    }
	    

	 
	    public void sendMessageNet(String msg) {
	    	
	    	InetAddress bcast = getBcastAddr();
    		
	    	
	    	if(bcast == null){
	    		return;
	    	}
	    	
	    	try {

				DatagramSocket s = new DatagramSocket(xpl_port);
				s.setBroadcast(true);
				
				String str = msg;
			    byte msgBytes[];
			    msgBytes = str.getBytes();
			    //DatagramPacket p1 = new DatagramPacket(b1, b1.length, bcast, xpl_port);
			    DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, bcast, xpl_port);
			    s.send(packet);
			    s.close();
			    		
			} catch (SocketException e) {
				Log.e(TAG,"Error sending (Socket):");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG,"Error sending (I/O):");
				e.printStackTrace();
			}
	    }

}
