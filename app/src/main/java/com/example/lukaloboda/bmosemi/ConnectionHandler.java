package com.example.lukaloboda.bmosemi;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.Handler;
import android.util.Log;
import android.widget.Adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nikolai5 on 5/10/16.
 */
public class ConnectionHandler{
    public static final String TAG = "ConnectionHandler";
    public static final String SERVICE_INSTANCE = "Talkie&Walkie";
    public static final String SERVICE_REG_TYPE="_audio._udp";

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public WifiP2pDnsSdServiceRequest serviceRequest;
    public WiFiDevicesAdapter adapter;

    Activity activity;
    Channel mChannel;
    WifiP2pManager mManager;


    public ConnectionHandler(WifiP2pManager mManager, Channel mChannel, Activity activity, WiFiDevicesAdapter adapter){
        this.activity=activity;
        this.mChannel=mChannel;
        this.mManager=mManager;
        this.adapter=adapter;
    }



    public void serviceRegistration(){
        // Record for service
        Map record = new HashMap();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        // Create service
        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_REG_TYPE, record);

        mManager.clearLocalServices(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });

        mManager.addLocalService(mChannel, serviceInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Service registration successful.");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Service registration failed.");
            }
        });
    }

    public void discoverServices(){

        // Prepare listener for discovering records
        DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
            }
        };

        // Prepare listener for discovering actual service
        DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice device) {
                if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                    WiFiP2pService service = new WiFiP2pService();
                    service.device = device;
                    service.instanceName = instanceName;
                    service.serviceRegistrationType = registrationType;
                    adapter.add(service);
                    Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
                }
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);



        // Create service discover request
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Service request successful.");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Service request failed.");
            }
        });


        // Run service discover
        mManager.discoverServices(mChannel, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discover successful.");
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.e(TAG, "P2P isn't supported on this device.");
                } else
                    Log.e(TAG, "Service discover failed.");
            }
        });

    }

    public void stopDiscoveringServices(){

        mManager.clearServiceRequests(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }



    public void connectP2p(WiFiP2pService service){
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if (serviceRequest != null)
            mManager.removeServiceRequest(mChannel, serviceRequest,
                    new ActionListener() {

                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(int arg0) {
                        }
                    });

        mManager.connect(mChannel, config, new ActionListener() {

            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int errorCode) {

            }
        });
    }

}

