package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Context;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import android.os.AsyncTask;
import android.util.Log;
/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;

    public final Uri providerUri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
    private int localSeq = 0;

    private int agreeSeq = 0;

    /*
     * This list is used as holdback queue as well as delivery queue.
     * The content in this list will never be removed, it will keep accumulating messages that are sent.
     */
    private ArrayList<MsgToSend> list = new ArrayList<MsgToSend>();

    private PriorityBlockingQueue<MsgToSend> holdbackQueue = new PriorityBlockingQueue<MsgToSend>(25, new Comparator<MsgToSend>(){
        @Override
        public int compare(MsgToSend lhs, MsgToSend rhs) {
            return (lhs.getSeq() - rhs.getSeq() == 0) ? (lhs.getPort().compareTo(rhs.getPort())) : (lhs.getSeq() - rhs.getSeq());
        }
    });

    private PriorityBlockingQueue<MsgToSend> deliverQueue = new PriorityBlockingQueue<MsgToSend>(25, new Comparator<MsgToSend>(){
        @Override
        public int compare(MsgToSend lhs, MsgToSend rhs) {
            return (lhs.getSeq() - rhs.getSeq() == 0) ? (lhs.getPort().compareTo(rhs.getPort())) : (lhs.getSeq() - rhs.getSeq());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
        }
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.e(TAG, "1111");
                String content = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + content); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                remoteTextView.append("\n");
                int id = 0;
                MsgToSend msg = new MsgToSend(content, 0, myPort, "firstcast",  id++);
            /*
             * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
             * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
             * the difference, please take a look at
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        //private int localMsgId = 0;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
        /*
         * TODO: Fill in your server code that receives messages and passes them
         * to onProgressUpdate().
         */
            //Log.e(TAG, "socket");
            try {
                synchronized (this) {
                    while (true) {
                        //get socket connection from serverSocket
                        Socket socket = serverSocket.accept();
                        //set time out, in case device crashes
                        //serverSocket.setSoTimeout(8000);
                        //Log.e(TAG, "accept");

                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        //Log.e(TAG, "Connected");
                        //convert String back into MsgToSend Object
                        MsgToSend msg = MsgToSend.toMessage((String) ois.readObject());
                        //Log.e("Server", msg.toString());

                        if (msg.isFirstCast()) {
                            //holdbackQueue.put(msg);
                            /*
                            first time receive a message
                            */
                            //compare local sequence in this processor and agreement sequence, set local sequence as the larger one and increment by one.
                            localSeq = Math.max(localSeq, agreeSeq) + 1;
                            //update message to the largest sequence in process
                            msg.setSeq(localSeq);
                            //send proposed message back
                            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                            oos.writeObject(msg.toString());
                            oos.flush();
                            oos.close();

                        } else if (msg.isMulticast()) {
                            /*
                            Second time receive a message, final message that need to be stored and display
                            */
                            //update agreement sequence if message has higher sequence
                            agreeSeq = Math.max(agreeSeq, msg.getSeq());
                            //set local sequence as message's sequence
                            localSeq = msg.getSeq();
                            /*
                            Why it's a list not a priority queue? I know that queue is more efficient, but I produces unexpected order
                            for some reason. I have messages with correct sequences but queue gives me a different order sometimes!!
                            Collections.sort() with the same comparator works perfect for that!!!!
                            I hate PriorityQueue!! It take me about 2 days to figure this shit out.
                             */
                            //put final message into a list
                            list.add(msg);
                            //sort the list according to the comparator when there are 16 or more elements in it
                            //this is purely for the sake of 'runtime', could have sort it every time
                            if(list.size() > 16){
                                Collections.sort(list, new Comparator<MsgToSend>() {
                                    @Override
                                    /*
                                    compare method, return positive number when lhs has greater sequence than rhs
                                    if their sequence are the same, compare their port number
                                     */
                                    public int compare(MsgToSend lhs, MsgToSend rhs) {
                                        return (lhs.getSeq() - rhs.getSeq() == 0) ? (lhs.getPort().compareTo(rhs.getPort())) : (lhs.getSeq() - rhs.getSeq());
                                    }
                                });
                                //store message in local and update the screen
                                for(int i = 0; i < list.size(); i++){
                                    //the first argument is the actual message, second argument is the message key, and last element is message sequence for testing purpose.
                                    publishProgress(list.get(i).getContent(), String.valueOf(i), String.valueOf(list.get(i).getSeq()));

                                }
                            }
                        }

                        //resource release
                        socket.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... strings) {
        /*
         * The following code displays what is received in doInBackground().
         */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived +"------------key: " +strings[1]+ "Seq: "+strings[2]+"\t\n");
        /*
         * The following code creates a file in the AVD's internal storage and stores a file.
         *
         * For more information on file I/O on Android, please take a look at
         * http://developer.android.com/training/basics/data-storage/files.html
         */
            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put("key", strings[1]);
            keyValueToInsert.put("value", strReceived);
            Log.e(TAG, providerUri.toString());
            getContentResolver().insert(
                    providerUri,    // assume we already created a Uri object with our provider URI
                    keyValueToInsert
            );

            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<MsgToSend, Void, Void> {


        @Override
        protected Void doInBackground(MsgToSend... msgs) {
            //Log.v(TAG, "client");
            MsgToSend msgToSend = null;

            //proposed sequence for this process
            int deliverSeq = 0;

            synchronized (this) {
                for (int i = 0; i < REMOTE_PORT.length; i++) {
                    try {
                        /*
                            sending message
                        */
                        //get current sending port
                        String remotePort = REMOTE_PORT[i];
                        Log.e("Client", remotePort);
                        //get message object
                        msgToSend = msgs[0];
                        //create socket
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                        //set socket time out in case device crashes
                        //socket.setSoTimeout(8000);
                        //Log.e("Client", msgToSend.toString());
                        //send message to server
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        //since serializable is allowed in this project, I have to convert it into string
                        oos.writeObject(msgToSend.toString());
                        oos.flush();

                        /*
                            receive proposed message from all 5 processors
                        */
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        //receive proposed message from processor
                        msgToSend = MsgToSend.toMessage((String) ois.readObject());
                        //when message comes back from server, get the largest sequence number of all message
                        deliverSeq = Math.max(msgToSend.getSeq(), deliverSeq);
                        //release resource
                        //oos.close();
                        //ois.close();
                        socket.close();
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        //handle IOException when one of the processor crashed, ignore this processor, continue running program
                        Log.e(TAG, "ClientTask socket IOException");
                        continue;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                }

                //update final delivery sequence number of this message
                msgToSend.setSeq(deliverSeq);
                //change message type to multicast
                msgToSend.setType("multicast");

                /*
                    deliver message to all processors
                 */
                for (int i = 0; i < REMOTE_PORT.length; i++) {
                    try {
                        //get current receiving port
                        String remotePort = REMOTE_PORT[i];
                        //Log.e(TAG, remotePort);

                        //create socket
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                        //set socket time out in case device crashes
                        //socket.setSoTimeout(8000);
                        //Log.e(TAG, msgToSend.toString());
                        //send final message
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        oos.writeObject(msgToSend.toString());
                        oos.flush();
                        //socket.close();
                        //release resource
                        //oos.close();
                        //
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        //handle IOException when one of the processor crashed, ignore this processor, continue running program
                        Log.e(TAG, "ClientTask socket IOException");
                        continue;
                    }

                }
            }

            return null;

        }
    }
}
