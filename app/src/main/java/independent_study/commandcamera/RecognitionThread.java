package independent_study.commandcamera;

import android.content.Context;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Blaine Huey on 3/13/2018.
 */

public class RecognitionThread extends Thread
{
    static final int SAMPLE_RATE = 16000;
    static final int SAMPLE_DURATION = 1000;
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION / 1000);
    private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 30;
    private static final String LABEL_FILENAME = "file:///android_asset/conv_actions_labels.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/conv_actions_frozen.pb";
    private static final String INPUT_DATA_NAME = "decoded_sample_data:0";
    private static final String SAMPLE_RATE_NAME = "decoded_sample_data:1";
    private static final String OUTPUT_SCORES_NAME = "labels_softmax";

    private TensorFlowInferenceInterface inferenceInterface;
    private ArrayList<String> labels;
    private ReentrantLock bufferLock;
    private short[] audioRecording;
    private int[] sharedOffset;
    private boolean pauseRecognition;
    private volatile boolean stopRecognition;

    public RecognitionThread(short[] audioRecording, Context context,
                             ReentrantLock bufferLock, int[] sharedOffset)
    {
        this.audioRecording = audioRecording;
        this.bufferLock = bufferLock;
        this.sharedOffset = sharedOffset;
        pauseRecognition = false;

        labels = new ArrayList<>();
        try
        {
            String actualFilename = LABEL_FILENAME.split("file:///android_asset/")[1];
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.conv_actions_labels)));
            String line;
            while ((line = br.readLine()) != null)
            {
                labels.add(line);
                if (line.charAt(0) != '_')
                {
                    //displayedLabels.add(line.substring(0, 1).toUpperCase() + line.substring(1));
                    Log.d("RecognitionThread", line.substring(0, 1).toUpperCase() + line.substring(1));
                }
            }
            br.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Problem reading label file!", e);
        }

        //inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILENAME);
        inferenceInterface = new TensorFlowInferenceInterface(context.getResources().openRawResource(R.raw.conv_actions_frozen));
        //inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), "conv_actions_frozen.pb");
    }

    @Override
    public void run()
    {
        short[] inputBuffer = new short[RECORDING_LENGTH];
        float[] floatInputBuffer = new float[RECORDING_LENGTH];
        float[] outputScores = new float[labels.size()];
        String[] outputScoresNames = new String[] {OUTPUT_SCORES_NAME};
        int[] sampleRateList = new int[] {SAMPLE_RATE};

        while(!stopRecognition)
        {
            if (pauseRecognition)
                return;

            bufferLock.lock();
            try
            {
                int maxLength = audioRecording.length;
                int firstCopyLength = maxLength - sharedOffset[0];
                int secondCopyLength = sharedOffset[0];
                System.arraycopy(audioRecording, sharedOffset[0], inputBuffer, 0, firstCopyLength);
                System.arraycopy(audioRecording, 0, inputBuffer, firstCopyLength, secondCopyLength);
            }
            finally
            {
                bufferLock.unlock();
            }

            for (int i = 0; i < RECORDING_LENGTH; ++i)
            {
                floatInputBuffer[i] = inputBuffer[i] / 32767.0f;
            }
            //Log.d("RecognizeThread", Arrays.toString(floatInputBuffer));

            inferenceInterface.feed(SAMPLE_RATE_NAME, sampleRateList);
            inferenceInterface.feed(INPUT_DATA_NAME, floatInputBuffer, RECORDING_LENGTH, 1);
            inferenceInterface.run(outputScoresNames);
            inferenceInterface.fetch(OUTPUT_SCORES_NAME, outputScores);

            long currentTime = System.currentTimeMillis();
            //TODO: Response to Sorting Identified Sounds
            Log.d("RecognitionThread", "Output: " + Arrays.toString(outputScores));

            int bestValueIndex = 0;
            float bestValue = outputScores[0];
            for(int i = 0; i < outputScores.length; i++)
            {
                if(outputScores[i] > bestValue)
                {
                    bestValue = outputScores[i];
                    bestValueIndex = i;
                }
            }

            int secondBestIndex = 0;
            float secondBestValue = 0;
            for(int i = 0; i < outputScores.length; i++)
            {
                if(i == bestValueIndex)
                    continue;
                if(outputScores[i] > secondBestValue)
                {
                    secondBestIndex = i;
                    secondBestValue = outputScores[i];
                }
            }

            if(secondBestValue > 0.4)
            {
                Log.d("RecognitionThread", "New Result: " + labels.get(secondBestIndex));
            }
            else
            {
                Log.d("RecognitionThread", "Result: " + labels.get(bestValueIndex));
            }

            try
            {
                sleep(MINIMUM_TIME_BETWEEN_SAMPLES_MS);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void setPause(boolean shouldPause)
    {
        pauseRecognition = shouldPause;
    }

    public void stopRecognition()
    {
        stopRecognition = true;
    }
}
