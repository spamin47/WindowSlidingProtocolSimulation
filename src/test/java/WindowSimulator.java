import java.util.Random;
public class WindowSimulator {

    public static void main(String[] args){
       System.out.println("Starting WindowSimulator...");
       //Check for invalid inputs
       if(args.length!=6){
           System.out.println("Invalid inputs. Insufficient amount of arguments");
           return;
       }
       int sws;
       int rws;
       int channel_length;
       float prob_not_recv;
       float prob_not_ackd;
       int num_frames;
       try{
           sws = Integer.parseInt(args[0]);
           rws = Integer.parseInt(args[1]);
           channel_length = Integer.parseInt(args[2]);
           prob_not_recv = Float.parseFloat(args[3]);
           prob_not_ackd = Float.parseFloat((args[4]));
           num_frames = Integer.parseInt(args[5]);

           if((sws >127 || sws<1) || (rws>127 || rws<1)){
               System.out.println("Invalid inputs. Invalid sws/rws arguments");
               return;
           }else if(prob_not_recv <0 || prob_not_recv>1 || prob_not_ackd < 0 || prob_not_ackd>1){
               System.out.println("Invalid inputs. Invalid prob_not_recv/prob_not_ackd");
               return;
           }
       } catch(NumberFormatException e){
           e.printStackTrace();
           System.out.println("Invalid inputs");
           return;
       }



        byte ack= -2; // 254 acknowledge frame
        byte fullbit = -1; // 255

        Station s1 = new Station(sws,rws, prob_not_recv);// sender
        Station r1 = new Station(sws,rws,prob_not_ackd); // receiver
        Pipe senderPipe = new Pipe(channel_length);
        Pipe receiverPipe = new Pipe(channel_length);
        int steps = 0;
        int counter = 0;
        boolean notDone = true;
        float sumUtilizations = 0;
        float averageUtilization;
        // frame sent by receiver held -- used to check if num_frame - 1th ack frame was sent
        byte[] receiverFrame;

        while(notDone)
        {
            System.out.println("\nStep " + steps);
            System.out.println("senderPipe");
            senderPipe.printContents();
            System.out.println("receiverPipe");
            receiverPipe.printContents();

            sumUtilizations += (float) (senderPipe.utilization() + receiverPipe.utilization())/2;
            // num frames ==> cmdline argument 
            if(counter < num_frames && r1.isReady())
            {
                s1.send(counter);
                counter++;
            }

            r1.receiveFrame(senderPipe.addFrame(s1.nextTransmitFrame()));
            receiverFrame = receiverPipe.addFrame(r1.nextTransmitFrame());
            s1.receiveFrame(receiverFrame);

            // compares value of each byte to check if its the appropriate ack frame(the final ack frame)
            if ( Byte.toUnsignedInt(receiverFrame[0]) == num_frames 
                && ((receiverFrame[1] & receiverFrame[2] & receiverFrame[3]) == fullbit) 
                && ((receiverFrame[4] & ack) == ack))
            {
                notDone = false;
                System.out.println("DONE");
            }
            else
            {
                steps++;    
            }
        }

        // computes average utilization -- since steps starts from 0, total # steps is actually steps + 1 ==> offset by 1 for average commputation
        averageUtilization = sumUtilizations/(steps + 1);
        System.out.println("Final Value of Steps: " + (steps));
        System.out.println("Average Pipe Utilization: " + averageUtilization);
    }
}
