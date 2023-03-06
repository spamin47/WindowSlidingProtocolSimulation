import java.awt.*;
import java.util.*;
public class Station {

    private byte[] senderBuffer;
    private int senderBuffer_startIndx; //empty slot start indx
    private int senderBuffer_endIndx; //empty slot end indx
    private int senderHead = 0; //start indx of next frame
    private byte senderBufferTimer[]; //-0 - idle, 10 - timer sets off
    private byte[] receiverBuffer;
    private int receiverBuffer_startIndx;//empty slot start indx
    private int receiverBuffer_endIndx;//empty slot end indx
    private int receiverHead = 0;//start indx of next frame


    private float propDrop; //chances of frame failing to be sent
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
        senderBufferTimer = new byte[sws];
    }

    //returns whether the Station can receive a new frame to queue.
    public boolean isReady(){
        //Check for any available frame slot within receiverBuffer.
        for(int i = 0;i<receiverBuffer.length;i+=5){
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
    public void updateTimer(){
        for(int i =0;i<senderBufferTimer.length;i++){
            if(senderBufferTimer[i]>0){
                senderBufferTimer[i]++;
            }
            System.out.println(senderBufferTimer[i]+" ");
        }
        System.out.println("\n");
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
        byte[] sendFrame = new byte[5];

        //0. ACK frame. 1. Resend an old frame whose timer went off. 2. Data frame in SenderWindow. 3. non-frame
        boolean cases[] = new boolean[3];

        //send ACK
        if(readyToSendACK){
            System.out.println("Transmitting ACK...\n");
            readyToSendACK = false;
            cases[0] = true;
            sendFrame= new byte[]{ack_sequenceNum,-1,-1,-1,-2};
        }else{ //Handle case 1 and 2
            int startIndx = -1;
            if(!cases[0]){
                int oldest = 9; //We're checking for any frames that are 10+ counter old to resend
                for(int i = 0;i<senderBufferTimer.length;i++){
                    if(senderBufferTimer[i]>oldest){
                        cases[1] = true;
                        oldest = senderBufferTimer[i];
                        startIndx = i;
                    }
                }
                if(startIndx>-1){
                    System.out.println("Retransmitting old data frame...");
                    senderBufferTimer[startIndx] =1; //reset timer
                    startIndx = startIndx * 5;
                }

            }
            if(!cases[1]){ //send data frame
                System.out.println("Transmitting data frame...");
                startIndx = senderHead;
                senderBufferTimer[startIndx/5] = 1;//start timer
                cases[2] = true;
            }
            int index =0;

            for(int i = startIndx;i<startIndx+5;i++){
                System.out.print(Integer.toBinaryString(senderBuffer[i])+  " ");
                sendFrame[index] = senderBuffer[i];
                index++;
            }
            System.out.println("\n");
            printSendBufferTimer();
        }

        //propDrop situation: send a nonframe (forces timer to go off and resend frame)
        if(Math.random()<propDrop){
            if(cases[0]){
                System.out.println("ACK frame transmission failed. PropDrop case encountered. Sending nonframe..\n");
                readyToSendACK = true;
            }else if(cases[1]){
                System.out.println("Resending old frame failed. PropDrop case encountered. Sending nonframe..\n");

            }else if(cases[2]){
                System.out.println("Transmission failed. PropDrop case encountered. Sending nonframe..\n");
            }

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
            for(int j = 0;j<senderBuffer.length;j+=5){
                if(senderBuffer[j] == frame[0] && senderBuffer[j]!= 0){ //ACK the right frame
                    System.out.println("Sequence number matches. Removing frame from senderBuffer...");
                    for(int i = j;i<j+5;i++){
                        senderBuffer[i] =0;
                    }
                    printSenderBuffer();
                    //adjust the sender head index
                    senderHead+=5;
                    if(senderHead>=senderBuffer.length){ //Index out of bound. Move index to 0
                        senderHead = 0;
                    }
                    senderBufferTimer[j/5] = 0; //remove timer for frame ACKed
                    printSendBufferTimer();
                    return;
                }
            }
            System.out.println("Sequence number does not match.");


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
            printSendBufferTimer();
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
    public void printSendBufferTimer(){
        System.out.println(this + " SendBufferTimer:");
        for(int i =0;i<senderBufferTimer.length;i++){
            System.out.print(senderBufferTimer[i] + " ");
        }
        System.out.println("\n");
    }
}
