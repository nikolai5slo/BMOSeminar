package com.example.lukaloboda.bmosemi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

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
                conHandler.connectP2p((WiFiP2pService) ((ListView) findViewById(R.id.seznam)).getItemAtPosition(position));
            }
        });

        conHandler.serviceRegistration();
        conHandler.discoverServices();
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

        Intent intent = new Intent(this, TalkActivity.class);
        intent.putExtra("IsOwner", p2pInfo.isGroupOwner);
        intent.putExtra("Address", p2pInfo.groupOwnerAddress.getHostAddress());

        startActivity(intent);
    }

}
