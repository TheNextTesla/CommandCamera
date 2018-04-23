package independent_study.commandcamera.audio;

import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import independent_study.commandcamera.CameraActivity;
import independent_study.commandcamera.R;

/**
 * Thread that Searches Through an Array to Find Voice Commands
 */
public class RecognitionThread extends Thread
{
    public static final int SAMPLE_RATE = 16000;
    public static final int SAMPLE_DURATION = 1000;
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION / 1000);
    private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 30;
    private static final String LABEL_FILENAME = "file:///android_asset/conv_actions_labels.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/conv_actions_frozen.pb";
    private static final String INPUT_DATA_NAME = "decoded_sample_data:0";
    private static final String SAMPLE_RATE_NAME = "decoded_sample_data:1";
    private static final String OUTPUT_SCORES_NAME = "labels_softmax";

    private CameraActivity context;
    private TensorFlowInferenceInterface inferenceInterface;
    private ArrayList<String> labels;
    private ReentrantLock bufferLock;
    private RecognizeCommands recognizeCommands;
    private short[] audioRecording;
    private int[] sharedOffset;
    private boolean pauseRecognition;
    private volatile boolean stopRecognition;

    /**
     * Creates the Thread
     * @param audioRecording - Shared Audio Recording Short[]
     * @param context - Android Context
     * @param labels - ArrayList of the Names Associated with Each Command
     * @param recognizeCommands - Object to Pump Output of Recognition for Determining Significance
     * @param bufferLock - Shared Resource Lock
     * @param sharedOffset - Cheap Way of Getting an Int Pointer to Share With Recording Thread
     */
    public RecognitionThread(short[] audioRecording, CameraActivity context, ArrayList<String> labels,
                             RecognizeCommands recognizeCommands, ReentrantLock bufferLock, int[] sharedOffset)
    {
        this.context = context;
        this.audioRecording = audioRecording;
        this.bufferLock = bufferLock;
        this.sharedOffset = sharedOffset;
        this.recognizeCommands = recognizeCommands;
        this.labels = labels;
        pauseRecognition = false;

        inferenceInterface = new TensorFlowInferenceInterface(context.getResources().openRawResource(R.raw.conv_actions_frozen));
    }

    /**
     * Runs Thread Loop Operation (Method Does NOT Loop)
     */
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

            //Operations on the Model's Detection Algorithm
            inferenceInterface.feed(SAMPLE_RATE_NAME, sampleRateList);
            inferenceInterface.feed(INPUT_DATA_NAME, floatInputBuffer, RECORDING_LENGTH, 1);
            inferenceInterface.run(outputScoresNames);
            inferenceInterface.fetch(OUTPUT_SCORES_NAME, outputScores);

            RecognizeCommands.RecognitionResult result = recognizeCommands.processLatestResults(outputScores, System.currentTimeMillis());
            Log.d("RecognitionThread", result.isNewCommand ? result.foundCommand : "None");

            if(result.isNewCommand && result.foundCommand.equals("go"))
            {
                Log.d("RecognitionThread", "go Detected, taking Picture");
                //context.setShouldTakePicture(); TODO: Add Again
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

    /**
     * Pauses the Recognition of the Thread
     * @param shouldPause - If Should Pause
     */
    public void setPause(boolean shouldPause)
    {
        pauseRecognition = shouldPause;
    }

    /**
     * Stops the Main Loop of the Thread
     */
    public void stopRecognition()
    {
        stopRecognition = true;
    }
}
