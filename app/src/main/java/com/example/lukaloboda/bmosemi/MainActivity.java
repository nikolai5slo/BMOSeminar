package com.example.lukaloboda.bmosemi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener{

    private final IntentFilter intentFilter = new IntentFilter();
    public static WifiP2pManager.Channel mChannel;
    public static WifiP2pManager mManager;
    private ConnectionHandler conHandler;
    private BroadcastReceiver mReceiver = null;
    private Activity mActivity;
    private Handler mHandler;
    private WiFiDevicesAdapter customAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        mHandler = new Handler();


        ListView seznam = (ListView) findViewById(R.id.seznam);
        customAdapter = new WiFiDevicesAdapter(this, R.id.seznam, new ArrayList<WiFiP2pService>());
        seznam.setAdapter(customAdapter);

        conHandler = new ConnectionHandler(mManager, mChannel, this, customAdapter);

        seznam.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
                stop();
                ((TextView)findViewById(R.id.status)).setText("Povezujem...");
                conHandler.connectP2p((WiFiP2pService) ((ListView) findViewById(R.id.seznam)).getItemAtPosition(position));
            }
        });

        conHandler.serviceRegistration();
        conHandler.discoverServices();
        mHandler.postDelayed(stopSearching, 20000);
    }

    Runnable stopSearching = new Runnable() {
        @Override
        public void run() {
            stop();
        }
    };

    private void stop(){
        ((TextView) findViewById(R.id.status)).setText("Found services");
        conHandler.stopDiscoveringServices();
        (findViewById(R.id.searching)).setVisibility(View.INVISIBLE);
        (findViewById(R.id.refreshButton)).setVisibility(View.VISIBLE);
    }

    private void restart(){
        ((TextView) findViewById(R.id.status)).setText("Finding services");
        (findViewById(R.id.refreshButton)).setVisibility(View.GONE);
        (findViewById(R.id.searching)).setVisibility(View.VISIBLE);
        customAdapter.clear();
        conHandler.discoverServices();
        mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
        mHandler.postDelayed(stopSearching, 20000);
    }

    public void restartDiscovery(View view){
        restart();
    }

    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, intentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        ((TextView) findViewById(R.id.status)).setText("Found services");
        conHandler.stopDiscoveringServices();
        (findViewById(R.id.searching)).setVisibility(View.INVISIBLE);

        Intent intent = new Intent(this, TalkActivity.class);
        intent.putExtra("IsOwner", p2pInfo.isGroupOwner);
        intent.putExtra("Address", p2pInfo.groupOwnerAddress.getHostAddress());

        startActivityForResult(intent, 1);
    }

    protected void onActivityResult(int reqC, int resC, Intent intent){
        if(reqC == 1){
            if(resC == RESULT_OK){
                stop();
                restart();
            }
        }
    }
}
