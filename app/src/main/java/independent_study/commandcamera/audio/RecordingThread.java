package independent_study.commandcamera.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread that Manages the Recording Operation
 * Gets Bytes From Microphone and Stores it In Array
 */
public class RecordingThread extends Thread
{
    private static final int SAMPLE_RATE = 16000;

    private short[] audioRecording;
    private ReentrantLock bufferLock;
    private int[] sharedOffset;
    private volatile boolean shouldRecord;

    /**
     * Constructor for Recording Thread
     * @param audioRecording - The short[] that the data is stored into
     *                       With the intention of this array being shared with other threads
     * @param bufferLock - Object Ensuring Consistent Usage Of Resources Between Threads
     * @param sharedOffset - Cheap Way of Getting an Int Pointer to Share With Other Thread
     */
    public RecordingThread(short[] audioRecording, ReentrantLock bufferLock, int[] sharedOffset)
    {
        this.audioRecording = audioRecording;
        this.bufferLock = bufferLock;
        this.sharedOffset = sharedOffset;
        shouldRecord = true;
    }

    /**
     * Runs Intended Operation on Thread
     */
    @Override
    public void run()
    {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if(bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE)
            bufferSize = SAMPLE_RATE * 2;

        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        if(record.getState() != AudioRecord.STATE_INITIALIZED)
        {
            Log.e("RecordingThread", "Audio State Failed");
            return;
        }

        record.startRecording();

        //Loops, For As Long as the Thread is Instructed to
        while(shouldRecord)
        {
            int numberRead = record.read(audioBuffer, 0, audioBuffer.length);
            int maxLength = audioRecording.length;
            int newRecordingOffset = sharedOffset[0] + numberRead;
            int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
            int firstCopyLength = numberRead - secondCopyLength;

            bufferLock.lock();
            try
            {
                System.arraycopy(audioBuffer, 0, audioRecording, sharedOffset[0], firstCopyLength);
                System.arraycopy(audioBuffer, firstCopyLength, audioRecording, 0, secondCopyLength);
                sharedOffset[0] = newRecordingOffset % maxLength;
            }
            finally
            {
                bufferLock.unlock();
            }
        }

        record.stop();
        record.release();
    }

    /**
     * Tells the Thread to Stop It's Main Loop Operation
     * Irreversibly Kills the Thread
     */
    public void stopRecording()
    {
        shouldRecord = false;
    }
}
