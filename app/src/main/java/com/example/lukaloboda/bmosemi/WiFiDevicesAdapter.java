package com.example.lukaloboda.bmosemi;

import android.content.ClipData;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Luka Loboda on 12-May-16.
 */
public class WiFiDevicesAdapter extends ArrayAdapter<WiFiP2pService> {

    private List<WiFiP2pService> items;

    public WiFiDevicesAdapter(Context context, int resource, List<WiFiP2pService> items) {
        super(context, resource, items);
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.list_item, null);
        }

        WiFiP2pService service = items.get(position);

        if (service != null) {
            TextView nap = (TextView) v.findViewById(R.id.imeNaprave);
            TextView sta = (TextView) v.findViewById(R.id.status);

            if(nap != null && sta != null){
                nap.setText(service.device.deviceName + " - " + service.instanceName);
                sta.setText(getDeviceStatus(service.device.status));
            }
        }

        return v;
    }

    public static String getDeviceStatus(int statusCode) {
        switch (statusCode) {
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }
}
