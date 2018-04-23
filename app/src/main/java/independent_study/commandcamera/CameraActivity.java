package independent_study.commandcamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import independent_study.commandcamera.audio.RecognitionThread;
import independent_study.commandcamera.audio.RecognizeCommands;
import independent_study.commandcamera.audio.RecordingThread;
import independent_study.commandcamera.opengl.OpenGLCameraRenderer;
import independent_study.commandcamera.opengl.OpenGLCameraSurface;
import independent_study.commandcamera.sms.BroadcastReceiverSMS;
import independent_study.commandcamera.sms.ListenerSMS;
import independent_study.commandcamera.util.FileSaveTask;

/**
 * @see "https://stackoverflow.com/questions/12519235/modifying-camera-output-using-surfacetexture-and-opengl#new-answer?newreg=30fb4db867854936807777c4920df9a8"
 */
public class CameraActivity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener, ListenerSMS
{
    private static final int PERMISSIONS_KEY = 308;
    private static final String SMS_ACTIVATION_KEY = "cheese";

    private static final long AVERAGE_WINDOW_DURATION_MS = 500;
    private static final float DETECTION_THRESHOLD = 0.70f;
    private static final int SUPPRESSION_MS = 1500;
    private static final int MINIMUM_COUNT = 2; //3
    private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 30;

    private Camera camera;
    private SurfaceTexture surfaceTexture;
    private OpenGLCameraSurface openGLCameraSurface;
    private OpenGLCameraRenderer openGLCameraRenderer;
    private BroadcastReceiverSMS broadcastReceiverSMS;
    private volatile boolean shouldTakePicture;

    private short[] recordingBuffer;
    private int[] sharedRecordingOffset;
    private ReentrantLock reentrantLock;
    private ArrayList<String> recognitionLabels;
    private RecordingThread recordingThread;
    private RecognitionThread recognitionThread;
    private RecognizeCommands recognizeCommands;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        shouldTakePicture = false;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        broadcastReceiverSMS = BroadcastReceiverSMS.getInstance();
        broadcastReceiverSMS.addListener(this);

        recordingBuffer = new short[(int) (RecognitionThread.SAMPLE_RATE * RecognitionThread.SAMPLE_DURATION / 1000.0)];
        reentrantLock = new ReentrantLock();
        sharedRecordingOffset = new int[1];
        recognitionLabels = new ArrayList<>();

        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.conv_actions_labels)));
            String line;
            while ((line = br.readLine()) != null)
            {
                recognitionLabels.add(line);
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

        recognizeCommands = new RecognizeCommands(recognitionLabels, AVERAGE_WINDOW_DURATION_MS, DETECTION_THRESHOLD,
                SUPPRESSION_MS, MINIMUM_COUNT, MINIMUM_TIME_BETWEEN_SAMPLES_MS);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, PERMISSIONS_KEY);
        }
        else
        {
            startView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        if(requestCode == PERMISSIONS_KEY)
        {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                startView();
            }
            else
            {
                System.exit(0);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onSMSReceived(SmsMessage message)
    {
        if(message != null && message.getMessageBody().toLowerCase().contains(SMS_ACTIVATION_KEY)
                && Math.abs(message.getTimestampMillis() - System.currentTimeMillis()) < 50000)
        {
            setShouldTakePicture();
        }
        Log.d("CameraActivity", "onSMSReceived");
    }

    private void startView()
    {
        openGLCameraSurface = new OpenGLCameraSurface(this);
        openGLCameraRenderer = openGLCameraSurface.getRenderer();

        setContentView(openGLCameraSurface);
        startMicrophone();
    }

    public void startCamera(int texture)
    {
        surfaceTexture = new SurfaceTexture(texture);
        surfaceTexture.setOnFrameAvailableListener(this);
        openGLCameraRenderer.setSurface(surfaceTexture);

        camera = Camera.open();
        try
        {
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();
        }
        catch (IOException ioe)
        {
            Log.w("CameraActivity","CAM LAUNCH FAILED");
        }
    }

    public void startMicrophone()
    {
        recordingThread = new RecordingThread(recordingBuffer, reentrantLock, sharedRecordingOffset);
        recognitionThread = new RecognitionThread(recordingBuffer,this, recognitionLabels, recognizeCommands, reentrantLock, sharedRecordingOffset);

        recordingThread.start();
        recognitionThread.start();
    }

    public void setShouldTakePicture()
    {
        shouldTakePicture = true;
    }

    public void onFrameAvailable(SurfaceTexture surfaceTexture)
    {
        openGLCameraSurface.requestRender();

        if(shouldTakePicture)
        {
            camera.takePicture(new Camera.ShutterCallback()
                               {
                                    public void onShutter()
                                    {
                                        try
                                        {
                                            AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                            mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
                                        }
                                        catch (NullPointerException npe)
                                        {
                                            npe.printStackTrace();
                                        }
                                    }
                               }, null ,new Camera.PictureCallback()
                               {
                                   @Override
                                   public void onPictureTaken(byte[] data, Camera camera)
                                   {
                                       Log.d("CameraActivity", "onPictureTaken");
                                       new FileSaveTask(data).execute();
                                       camera.stopPreview();
                                       camera.startPreview();
                                   }
                               }
            );

            shouldTakePicture = false;
        }

        //NativeBridge.getInstance().runCameraOperations(camera.getParameters().getPreviewSize().height, camera.getParameters().getPreviewSize().width);
        //Log.d("CameraActivity", "onFrameAvailable");
    }

    @Override
    public void onResume()
    {
        if(camera != null)
        {
            try
            {
                camera.reconnect();
                camera.startPreview();
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
        super.onResume();
    }

    @Override
    public void onPause()
    {
        if(camera != null)
        {
            camera.stopPreview();
        }
        super.onPause();
    }

    @Override
    public void onDestroy()
    {
        if(camera != null)
        {
            camera.release();
        }
        super.onDestroy();
    }
}
