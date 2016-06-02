package com.example.lukaloboda.bmosemi;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nikolai5 on 5/11/16.
 */
class AudioStreamer implements Runnable{
    int port;
    private volatile List<InetAddress> clients = new LinkedList<>();
    private volatile boolean running=false;
    Context mContext;
    public static final String TAG ="AudioStreamer";
    private Thread thread=null;
    private volatile AudioRecord recorder=null;

    private int sampleRate = 8000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    public AudioStreamer(int port, Context mContext, InetAddress groupOwner) throws SocketException {
        this.port=port;
        this.mContext=mContext;
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,sampleRate,channelConfig,audioFormat,minBufSize);
    }

    public synchronized void setClients(List<InetAddress> clients){
        this.clients.clear();
        this.clients.addAll(clients);
        Log.d(TAG, "Added new clients to streamer "+this.clients.size());
    }

    /*
    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
          quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }
    */



    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions((Activity)mContext, new String[]{Manifest.permission.RECORD_AUDIO}, 20);
        }
        //recorder = findAudioRecord();
        try {
            if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                throw new Exception("Can't initialize audio recorder");
            }

            recorder.startRecording();

            //DatagramPacket packet = new DatagramPacket(data, data.length);

            try {
                DatagramSocket socket = new DatagramSocket();
                while (true) {
                    if(!running) break;
                    byte[] data = new byte[1024];
                    minBufSize = recorder.read(data, 0, data.length);

                    Log.d(TAG, "Num clients "+clients.size());
                    for(InetAddress c: clients){
                        DatagramPacket packet = new DatagramPacket(data, data.length, c, port);
                        Log.d(TAG, "Sending " + c.getHostAddress());
                        socket.send(packet);
                    }

                }
                socket.close();
                Log.d(TAG, "Streamer stopped");
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            recorder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void startRec(){
        if(!running) {
            running = true;
            thread = new Thread(this);
            thread.start();
        }
    }
    void stopRec(){
        if(running) {
            running = false;
        }
    }

    void cancel(){
        if(recorder!=null){
            stopRec();
        }
        clients.clear();
    }

    protected void finalize(){
        cancel();
    }

}
