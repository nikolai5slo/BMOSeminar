package com.example.lukaloboda.bmosemi;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiManager;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nikolai5 on 5/10/16.
 */
public class AudioInputStream implements Runnable{
    public final static String TAG = "AudioInputStream";
    private volatile LinkedList<InetAddress> clients=new LinkedList<>();
    private int port;
    private Context mContext;
    private AudioTrack track;
    private Thread thread;
    public AudioInputStream(int udpPort, Context mContext){
        this.port=udpPort;
        this.mContext=mContext;
        track = new  AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelConfig, audioFormat , minBufSize, AudioTrack.MODE_STREAM);
    }

    private int sampleRate = 8000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    private volatile boolean running=false;

    public synchronized void setClients(List<InetAddress> clients){
        this.clients.clear();
        this.clients.addAll(clients);
    }

    @Override
    public void run() {
        Log.d(TAG, "Server handler run...");
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        try {
            DatagramSocket serverSocket = new DatagramSocket(port);
            serverSocket.setSoTimeout(500);
            //serverSocket.setSoTimeout(15000); //15 sec wait for the client to connect
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            if(track.getState() == AudioTrack.STATE_UNINITIALIZED){
                throw new Exception("Can't initialize audio track");
            }
            track.play();

            while(this.running) {
                try {
                    serverSocket.receive(packet);
                    for (InetAddress c : clients) {
                        DatagramPacket n= new DatagramPacket(data, data.length, c, port);
                        serverSocket.send(n);
                        Log.d(TAG, "Passing " + c.getHostAddress());
                    }

                    track.write(data, 0, data.length);
                }catch (SocketTimeoutException e){

                }catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            Log.d(TAG, "Receiver stopped");
            serverSocket.close();
            track.stop();
        }catch (SocketException e){
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void start(){
        if(!this.running){
            this.running=true;
            this.thread=new Thread(this);
            this.thread.start();
            Log.d(TAG, "Thread STARTED");
        }
    }

    protected void finalize(){
        cancel();
    }

    public void cancel(){
        if(this.running) {
            Log.d(TAG, "Halt receiver");
            clients.clear();
            this.running = false;
            Log.d(TAG, "Halt receiver "+this.running);
        }
    }

}
