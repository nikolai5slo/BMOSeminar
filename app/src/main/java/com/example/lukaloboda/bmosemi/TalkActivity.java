package com.example.lukaloboda.bmosemi;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.ArraySet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import android.os.Handler;

public class TalkActivity extends AppCompatActivity {
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    public static final String TAG = "TalkActivity";
    private volatile AudioInputStream receiver=null;
    private volatile AudioStreamer streamer=null;
    private Handler mDiscovery = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "create");
        setContentView(R.layout.talk_screen);


        Intent i = getIntent();
        final boolean isOwner = i.getBooleanExtra("IsOwner", false);
        try {
            InetAddress address = InetAddress.getByName(i.getStringExtra("Address"));


            receiver = new AudioInputStream(2349, this);
            receiver.start();
            streamer = new AudioStreamer(2349, this);


            if(isOwner){
                Log.d(TAG, "Connected as owner");
                ConnectionAccept connA = new ConnectionAccept(new ConnectionAccept.ConnectionListener() {
                    @Override
                    public void connectionListChange(List<InetAddress> clients) {
                        streamer.setClients(clients);
                        receiver.setClients(clients);
                    }
                });
                connA.start();
            }else
            {
                Log.d(TAG, "Connected ad client.");
                streamer.setClients(Arrays.asList(new InetAddress[]{address}));
                receiver.setClients(new LinkedList<InetAddress>());

                (new AsyncTask<InetAddress, Integer, Object>() {
                    @Override
                    protected Object doInBackground(InetAddress... addr) {
                        try {
                            Socket s = new Socket(addr[0], TCP_SERVER_PORT);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }).execute(address);
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }


        Button tB = (Button) findViewById(R.id.talkButton);
        tB.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    streamer.startRec();
                    Log.d(TAG, "Action press");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    streamer.stopRec();
                    Log.d(TAG, "Action release");
                }
                return false;
            }
        });

        //mDiscovery.postDelayed(discoverCode, 2000);


    }



    private static final int TCP_SERVER_PORT = 2315;
    private static class ConnectionAccept extends Thread{
         interface ConnectionListener{
           void connectionListChange(List<InetAddress> cons);
         }
        private LinkedHashSet<InetAddress> clients = new LinkedHashSet<>();
        private ConnectionListener listener = null;
        public void setConnListener(ConnectionListener l){
            listener = l;
        }

        ConnectionAccept(ConnectionListener l){
            setConnListener(l);
        }

        @Override
        public void run() {
            running = true;
            try {
               ServerSocket ss = new ServerSocket(TCP_SERVER_PORT);
               while(true){
                   synchronized (this){
                       if(!running) break;
                   }
                   Socket s = ss.accept();

                   clients.add(s.getInetAddress());
                   listener.connectionListChange(Arrays.asList(clients.toArray(new InetAddress[]{})));

                   Log.d(TAG, "New conn" + s.getInetAddress());
                   s.close();
               }
               ss.close();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }

        void cancel(){
            running = false;
        }
        private volatile boolean running = true;
    }


     /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        Log.d(TAG, "Resume");
        super.onResume();
        /*
        if(receiver==null)
            receiver = new AudioInputStream(2349, this);
        receiver.start();
        try {
            if(streamer==null)
                streamer = new AudioStreamer(2349, this);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        */
    }

    @Override
    public void onPause() {
        Log.d(TAG, "Pause");
        super.onPause();
        /*
        if(receiver!=null){
            receiver.cancel();
            receiver=null;
        }
        if(streamer!=null){
            streamer.cancel();
            streamer=null;
        }
        */
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.d(TAG, "Stop");
        /*
        super.onStop();
        if(receiver!=null){
            receiver.cancel();
            receiver=null;
        }
        if(streamer!=null){
            streamer.cancel();
            streamer=null;
        }
        */
        if(mManager!=null && mChannel!=null){
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                   Log.d(TAG, "Successfully disconnected from group.");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Disconnection from group failed.");
                }
            });
        }
    }
}
