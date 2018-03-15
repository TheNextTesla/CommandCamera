package independent_study.commandcamera;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Blaine Huey on 3/13/2018.
 */

public class RecordingThread extends Thread
{
    private static final int SAMPLE_RATE = 16000;

    private short[] audioRecording;
    private ReentrantLock bufferLock;
    private int[] sharedOffset;
    private volatile boolean shouldRecord;

    public RecordingThread(short[] audioRecording, ReentrantLock bufferLock, int[] sharedOffset)
    {
        this.audioRecording = audioRecording;
        this.bufferLock = bufferLock;
        this.sharedOffset = sharedOffset;
        shouldRecord = true;
    }

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

    public void stopRecording()
    {
        shouldRecord = false;
    }
}
