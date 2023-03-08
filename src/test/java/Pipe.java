

public class Pipe {
    private static final int FRAME_SIZE = 5;
    private int channelLength;
    private byte[] channel;

    Pipe(int channelLength)
    {
        this.channelLength = channelLength;
        channel = new byte[FRAME_SIZE * channelLength];
    }

    /*
     * shifts right by one frame(5 bytes) and adds new frame
     * returns: last frame removed by the shift
     */
    byte[] addFrame(byte[] frame)
    {
        byte[] ret = new byte[5];
        int index = FRAME_SIZE - 1;

        //shifts right 5 times
        while(index >= 0)
        {
            // records last element
            ret[index] = channel[channel.length - 1];
    
            // shifts right once
            for(int i = channel.length - 1; i > 0; i--)
            {
                channel[i] = channel[i - 1];
            }

            index--; 
        }

        // replaces first 5 elements with frame
        for(int i = 0; i < FRAME_SIZE; i++)
        {
            channel[i] = frame[i];
        }
        
        return ret; 
    }

    float utilization()
    {
        byte nonframe = -1;
        int count = 0;

        // iterates through frames
        for (int i = 0; i < channelLength; i++)
        {
            // iterates through each byte in a frame
            for(int j = 0; j < FRAME_SIZE; j++)
            {
                byte b = channel[(FRAME_SIZE * i) + j];
                // if a byte in frame is non zero, count it towards utilization
                if (Byte.compare(nonframe, b) != 0)
                {
                    count++;
                    break;
                }
            }
        }
        
        System.out.println(count);
        return (float) count/channelLength;
    }

    void printContents()
    {
        StringBuilder sb = new StringBuilder();
        
        for(byte b : channel)
        {
            sb.append(String.format("%02X ", b));
        }
        System.out.println(sb.toString());
    }
}
