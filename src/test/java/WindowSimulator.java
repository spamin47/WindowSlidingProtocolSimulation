import java.util.Random;
public class WindowSimulator {

    public static void main(String[] args){
//        System.out.println("Starting WindowSimulator...");
//        //Check for invalid inputs
//        if(args.length!=6){
//            System.out.println("Invalid inputs. Insufficient amount of arguments");
//            return;
//        }
//        int sws;
//        int rws;
//        int channel_length;
//        float prob_not_recv;
//        float prob_not_ackd;
//        int num_frames;
//        try{
//            sws = Integer.parseInt(args[0]);
//            rws = Integer.parseInt(args[1]);
//            channel_length = Integer.parseInt(args[2]);
//            prob_not_recv = Float.parseFloat(args[3]);
//            prob_not_ackd = Float.parseFloat((args[4]));
//            num_frames = Integer.parseInt(args[5]);
//
//            if((sws >127 || sws<1) || (rws>127 || rws<1)){
//                System.out.println("Invalid inputs. Invalid sws/rws arguments");
//                return;
//            }else if(prob_not_recv <0 || prob_not_recv>1 || prob_not_ackd < 0 || prob_not_ackd>1){
//                System.out.println("Invalid inputs. Invalid prob_not_recv/prob_not_ackd");
//                return;
//            }
//        }catch(NumberFormatException e){
//            e.printStackTrace();
//            System.out.println("Invalid inputs");
//            return;
//        }
        byte frame= 4;
        byte ack= -2; //254 acknowledge frame
        byte fullbit = -1; //255
        boolean ackowledged_frame = (ack &frame) == ack;

        System.out.println(ack);
        System.out.println(ackowledged_frame);
        byte test = (byte)0xffed10cd;
        System.out.println(test);
        int test2 = 0xffed10cd >> 8;
        byte test3 = (byte)test2;
        System.out.println(test3);
        Station s1 = new Station(1,1,0);
        s1.send(6783);


    }

}
