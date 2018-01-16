/*********************************************
 * ANDROID SOUND PRESSURE METER APPLICATION
 * DESC   : Recording Thread that calculates SPL.  
 * WEBSRC : Recording : http://www.anddev.org/viewtopic.php?p=22820
 * AUTHOR : hashir.mail@gmail.com
 * DATE   : 19 JUNE 2009
 * CHANGES: - Changed the recording logic
 * 			- Added logic to pass recorded buffer to FFT
 * 			- Added logic to calculate SPL.
 *********************************************/

package android.ilus.spltest.android.ilus;

import java.math.BigDecimal;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;

public class Recorder implements Runnable
{
 
    private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MY_MSG = 1;
    private int channelConfiguration;;

    private int frequency;
    Handler handle;
    private volatile boolean isRecording = false;

    short[] tempBuffer;

    /**
     * Handler is passed to pass messages to main screen Recording is done
     * 8000Hz MONO 16 bit
     */
    public Recorder(Handler h)
    {
        super();
        this.setFrequency(8000);
        this.setChannelConfiguration(AudioFormat.CHANNEL_CONFIGURATION_MONO);
        this.handle = h;
    }

    /**
     * @return the audioEncoding
     */
    public int getAudioEncoding()
    {
        return audioEncoding;
    }

    /**
     * @return the channelConfiguration
     */
    public int getChannelConfiguration()
    {
        return channelConfiguration;
    }

    /**
     * @return the frequency
     */
    public int getFrequency()
    {
        return frequency;
    }

    
    /**
     * @return the isRecording
     */
    public boolean isRecording()
    {

        return isRecording;

    }

    /**
     * Calculate SPL P = square root ( 2*Z*I ) - > Pressure Z = Acoustic
     * Impedance = 406.2 for air at 30 degree celsius I = Intensity = 2*Z*pi
     * square*frequency square*Amplitude square
     * 
     * @param bsize
     *            - the size of FFT required.
     */
    public double measure(int bsize)
    {
        int i = 0;
        double frequency = 0;
        double amplitude = 0;
        double max = 0.0;
        int max_index = 0;
        double w = 0.0;

        double Z = 406.2;
        double I = 0.0;
        double P = 0.0;
        double P0 = 2 * 0.00001; // is constant
        double Istar = 0.0; // SPL

        Complex[] x = new Complex[bsize];

        for (i = 0; i < bsize; i++)
        {
            x[i] = new Complex(tempBuffer[i], 0);
        }

        Complex[] xf = new Complex[bsize];
        xf = FFT.fft(x);

        for (i = 1; i < bsize / 2; i++)
        {
            w = xf[i].abs();
            if (w > max)
            {
                max_index = i;
                max = w;
            }
        }
        // Frequency and Amp of fundamental frequency
        frequency = max_index * bsize;
        amplitude = max * 2 / bsize;

        I = Z * 2 * 2 * Math.PI * Math.PI * frequency * frequency * amplitude
                * amplitude;
        P = Math.sqrt(Z * I);
        if (P != 0)
            Istar = round(20 * Math.log10(P / P0) / 10, 3); // divide by 10 to
                                                            // correct the
                                                            // calculation

        Message msg = handle.obtainMessage(MY_MSG, "\n\nFrequency = " + frequency + " Hz\n"
                + Istar + " db SPL");
        handle.sendMessage(msg);
        return Istar;

    }

    /**
     * Utility Function to round a double value
     * 
     * @param d
     *            - The decimal value
     * @param decimalPlace
     *            - how many places required
     * @return double - the rounded value
     */
    public double round(double d, int decimalPlace)
    {
        // see the Javadoc about why we use a String in the constructor
        // http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }

    /* Recording THREAD */
    public void run()
    {

        AudioRecord recordInstance = null;

        // We're important...
        android.os.Process
                .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        short bufferSize = 4096;// 2048;

        recordInstance = new AudioRecord(MediaRecorder.AudioSource.MIC, this
                .getFrequency(), this.getChannelConfiguration(), this
                .getAudioEncoding(), bufferSize);

        tempBuffer = new short[bufferSize];
        recordInstance.startRecording();
        // Continue till STOP button is pressed.

        while (this.isRecording)
        {
            @SuppressWarnings("unused")
			double splValue = 0.0;

            for (int i = 0; i < tempBuffer.length; i++)
            {
                tempBuffer[i] = 0;
            }

            recordInstance.read(tempBuffer, 0, bufferSize);
            splValue = measure(bufferSize); // calucalte SPL
        }
        // STOP BUTTON WAS PRESSED.
        // Close resources...
        recordInstance.stop();

    }

    /**
     * @param channelConfiguration
     *            the channelConfiguration to set
     */
    public void setChannelConfiguration(int channelConfiguration)
    {
        this.channelConfiguration = channelConfiguration;
    }

    /**
     * @param frequency
     *            the frequency to set
     */
    public void setFrequency(int frequency)
    {
        this.frequency = frequency;
    }

    /**
     * @param isRecording
     *            the isRecording to set
     */
    public void setRecording(boolean isRecording)
    {

        this.isRecording = isRecording;

    }

}
