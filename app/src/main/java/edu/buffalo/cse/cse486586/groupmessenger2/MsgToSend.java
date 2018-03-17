package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by rong on 3/5/18.
 * Class that packs a message
 */

public class MsgToSend implements Comparable<MsgToSend>{
    //actual message content
    private String content;
    //sequence number of this message
    private int seq;
    //port that sends this message
    private String port;
    //type of this message, either proposal or multicast
    private String type;

    private int id;
    public MsgToSend(String content, int seq, String port, String type, int id) {
        this.content = content;
        this.seq = seq;
        this.port = port;
        this.type = type;
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public int getSeq() {
        return seq;
    }

    public String getPort() {
        return port;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return content+"-"+seq+"-"+port+"-"+type+"-"+id+"-";
    }

    /**
     * convert string to MsgToSend object
     * @param msg message object in form of string
     * @return
     */
    public static MsgToSend toMessage(String msg){
        String[] msgs= msg.split("-");
        return new MsgToSend(msgs[0], Integer.parseInt(msgs[1]), msgs[2], msgs[3], Integer.parseInt(msgs[4]));
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public boolean isFirstCast(){
        return "firstcast".equals(this.type);
    }

    public boolean isMulticast(){
        return "multicast".equals(this.type);
    }

    @Override
    public int compareTo(MsgToSend another) {
        if(seq > another.getSeq()){
            return 1;
        }else if(seq < another.getSeq()){
            return -1;
        }else{
            int portCompare = port.compareTo(another.port);
            if(portCompare == 0){
                portCompare = content.compareTo(another.getContent());
            }
            return portCompare;
        }
    }
}
