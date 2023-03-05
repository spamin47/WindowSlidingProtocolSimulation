import java.awt.*;
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
    private byte sequenceNum = 1; //start at 1 because 0 indicates empty frame slot


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
        //Check for any available frame slot within receiverBuffer.
        for(int i = 0;i<receiverBuffer.length;i++){
            if(receiverBuffer[receiverBuffer_startIndx] ==0){
                receiverBuffer_endIndx = receiverBuffer_startIndx+4;
                System.out.println(this + " is ready to receive data at indx: "+ receiverBuffer_startIndx +"-" + receiverBuffer_endIndx);
                printReceiverBuffer();
                return true;
            }
            receiverBuffer_startIndx+=5;
            if(receiverBuffer_startIndx>=receiverBuffer.length){
                receiverBuffer_startIndx = 0;
            }
        }
        System.out.println("Receiverbuffer full. Cannot receive any data at the moment.\n");
        return false;
    }

    //packages data as a 5 byte frame, writes it to the appropriate senderBuffer bytes.
    public boolean send(int data){ // 0xff ff ff ff
        System.out.println("Sending " + data + " as " + Integer.toBinaryString(data) + " to senderbuffer of " +this);
        int count = 0;
        //store each byte into the senderBuffer byte array
        for(int i = senderBuffer_endIndx;i>=senderBuffer_startIndx || count<5;i--){
            senderBuffer[i] = (byte)data; //store byte
            data = data>>8; //onto next byte (8 bits)

            if(count ==4){ //set header byte with sequence number
                senderBuffer[i] = sequenceNum;
                sequenceNum++;
            }
            count++;

        }
        printSenderBuffer();
        //Move the sender window
        senderBuffer_endIndx+=5; //frame size 5 data
        senderBuffer_startIndx+=5;
        if(senderBuffer_endIndx>senderBuffer.length){//index out of bound case
            senderBuffer_startIndx = 0;
            senderBuffer_endIndx = 4;
        }
        return true;
    }
    //chooses the next frame that will be sent from the Station
    public byte[] nextTransmitFrame(){
        System.out.println(this + " Next Transmit Frame.");

        //send ACK
        if(readyToSendACK){
            System.out.println("Transmitting ACK...\n");
            readyToSendACK = false;
            return new byte[]{ack_sequenceNum,-1,-1,-1,-2};
        }
        byte[] sendFrame = new byte[5];
        int index =0;
        System.out.println("Transmitting data frame...");
        for(int i = senderHead;i<senderHead+5;i++){
            System.out.print(Integer.toBinaryString(senderBuffer[i])+  " ");
            sendFrame[index] = senderBuffer[i];
            index++;
        }
        System.out.println("\n");

        //propDrop situation: send a nonframe (forces timer to go off and resend frame)
        if(Math.random()<propDrop){
            System.out.println("Transmission failed. PropDrop case encountered. Sending nonframe..\n");
            return new byte[]{-1,-1,-1,-1,-1}; //nonFrame
        }

        return sendFrame;
    }
    //
    public void receiveFrame(byte[] frame){
        System.out.println(this + " Received frame:");
        WindowSimulator.printFrame(frame);

        byte ack = -2; //254 = 0b1111 1110 = -2
        byte fullbit = -1;//0b11111111

        if((frame[4]&ack)==ack && (frame[1] & frame[2] & frame[3]) == fullbit){ //acknowledgment frame received
            System.out.println("ACK frame received.");
            if(senderBuffer[senderHead] == frame[0] && senderBuffer[senderHead]!= 0){ //ACK the right frame
                System.out.println("Sequence number matches. Removing frame from senderBuffer...");
                for(int i = senderHead;i<senderHead+5;i++){
                    senderBuffer[i] =0;
                }
                printSenderBuffer();
                //adjust the sender head index
                senderHead+=5;
                if(senderHead>=senderBuffer.length){ //Index out of bound. Move index to 0
                    senderHead = 0;
                }

            }else{
                System.out.println("Sequence number does not match.");
            }

        }else if((frame[0]&frame[1] & frame[2] & frame[3]&frame[4]) == fullbit){ //nonframe received
            System.out.println("Nonframe detected. Do nothing.");
        }else{ //data frame received
            System.out.println("Storing data frame in receiverBuffer...");
            int count = 0;
            //store each byte into the byte array
            for(int i = receiverBuffer_startIndx;i<=receiverBuffer_endIndx || count<5;i++){
                receiverBuffer[i] = frame[count];
                count++;
            }

            //Move the receiver window to an empty slot
            receiverBuffer_endIndx+=5; //frame size 5 data
            receiverBuffer_startIndx+=5;
            if(receiverBuffer_endIndx>=receiverBuffer.length){
                receiverBuffer_startIndx = 0;
                receiverBuffer_endIndx = 4;
            }

            //ACK handling - once received frame, set ACK for that frame
            ack_sequenceNum = frame[0];
            readyToSendACK = true;
            printReceiverBuffer();
        }
    }

    public void printSenderBuffer(){
        System.out.println("Senderbufffer:");
        for(int i =0;i<senderBuffer.length;i++){
            if(i%5 == 0){
                System.out.print(" |"+senderBuffer[i] + "|");
            }else{
                System.out.print(senderBuffer[i] + "|");
            }

        }
        System.out.println("");
        for(int i =0;i<senderBuffer.length;i++){
            if(i%5 == 0){
                System.out.print(" |"+Integer.toBinaryString(senderBuffer[i]) + "|");
            }else{
                System.out.print(Integer.toBinaryString(senderBuffer[i]) + "|");
            }

        }
        System.out.println("\n");
    }
    public void printReceiverBuffer(){
        System.out.println("Receiverbuffer:");
        for(int i =0;i<receiverBuffer.length;i++){
            if(i%5 == 0){
                System.out.print(" |"+receiverBuffer[i] + "|");
            }else{
                System.out.print(receiverBuffer[i] + "|");
            }

        }
        System.out.println("");
        for(int i =0;i<receiverBuffer.length;i++){
            if(i%5 == 0){
                System.out.print(" |"+Integer.toBinaryString(receiverBuffer[i]) +"|");
            }else{
                System.out.print(Integer.toBinaryString(receiverBuffer[i]) +"|");
            }

        }
        System.out.println("\n");
    }
}
