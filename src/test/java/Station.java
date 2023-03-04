import java.util.*;
public class Station {

    private byte[] senderBuffer;
    private int senderBuffer_startIndx;
    private int senderBuffer_endIndx;
    private byte[] receiverBuffer;
    private int receiverBuffer_startIndx;
    private int receiverBuffer_endIndx;
    private int senderHead = 0;
    private int receiverHead = 0;

    private float propDrop;
    private byte ack_sequenceNum= 0;
    private boolean readyToSendACK = false;
    private int maxSequence;


    public Station(int sws,int rws, float propDrop){
        senderBuffer = new byte[sws*5];
        receiverBuffer = new byte[rws*5];
        senderBuffer_startIndx = 0;
        senderBuffer_endIndx = 4;
        receiverBuffer_startIndx =0;
        receiverBuffer_endIndx = 4;
        this.propDrop = propDrop;
        maxSequence = sws*2;
    }

    //returns whether the Station can receive a new frame to queue.
    public boolean isReady(){
        return receiverBuffer[receiverBuffer_startIndx] ==0;
    }
    //packages data as a 5 byte frame, writes it to the appropriate senderBuffer bytes.
    public boolean send(int data){ // 0xff ff ff ff
        int count = 0;
        //store each byte into the byte array
        for(int i = senderBuffer_endIndx;i>=senderBuffer_startIndx || count<5;i--){
            senderBuffer[i] = (byte)data; //store byte
            data = data>>8; //onto next byte (8 bits)
            count++;
        }
//        System.out.println("senderBuffer:");
//        for(int i = 0;i<senderBuffer.length;i++){
//            System.out.print(Integer.toBinaryString(senderBuffer[i]) +" ");
//        }
//        System.out.println("");
        if(count!=5){
            return false;
        }

        //Move the sender window
        senderBuffer_endIndx+=5; //frame size 5 data
        senderBuffer_startIndx+=5;
        if(senderBuffer_endIndx>senderBuffer.length){
            senderBuffer_startIndx = 0;
            senderBuffer_endIndx = 4;
        }
        return true;
    }
    //chooses the next frame that will be sent from the Station
    public byte[] nextTransmitFrame(){
        //send ACK
        if(readyToSendACK){
            readyToSendACK = false;
            return new byte[]{ack_sequenceNum,-1,-1,-1,-2};
        }
        byte[] sendFrame = new byte[5];
        int index =0;
        for(int i = senderHead;i<senderHead+5;i++){
            sendFrame[index] = senderBuffer[i];
            index++;
        }

        //propDrop situation: send a nonframe (forces timer to go off and resend frame)
        if(Math.random()<propDrop){
            return new byte[]{-1,-1,-1,-1,-1}; //nonFrame
        }

        return sendFrame;
    }
    //
    public void receiveFrame(byte[] frame){
        byte ack = -2; //254 = 0b1111 1110
        byte fullbit = -1;//0b11111111
        if((frame[4]&ack)==ack && (frame[1] & frame[2] & frame[3]) == fullbit){ //ackowledgement frame received
            if(senderBuffer[senderHead] == frame[0]){
                System.out.println("Sequence number matches.");
            }
            for(int i = senderHead;i<senderHead+5;i++){
                senderBuffer[i] =0;
            }
            //adjust the sender head index
            senderHead+=5;
            if(senderHead>senderBuffer.length){
                senderHead = 0;
            }
        }else if((frame[0]&frame[1] & frame[2] & frame[3]&frame[4]) == fullbit){ //nonframe received
            System.out.println("Nonframe detected.");
        }else{ //data frame received
            int count = 0;
            //store each byte into the byte array
            for(int i = receiverBuffer_endIndx;i>=receiverBuffer_startIndx || count<5;i--){
                receiverBuffer[i] = frame[count];
                count++;
            }

            //Move the receiver window to an empty slot
            receiverBuffer_endIndx+=5; //frame size 5 data
            receiverBuffer_startIndx+=5;
            if(receiverBuffer_endIndx>receiverBuffer.length){
                receiverBuffer_startIndx = 0;
                receiverBuffer_endIndx = 4;
            }

            //ACK handling - once received frame, set ACK for that frame
            ack_sequenceNum = frame[0];
            readyToSendACK = true;

        }
    }
}
