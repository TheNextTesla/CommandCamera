package independent_study.commandcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.graphics.SurfaceTexture;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @see "https://stackoverflow.com/questions/12519235/modifying-camera-output-using-surfacetexture-and-opengl#new-answer?newreg=30fb4db867854936807777c4920df9a8"
 */
public class CameraActivity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener
{
    private static final int PERMISSIONS_KEY = 308;

    private Camera camera;
    private SurfaceTexture surfaceTexture;
    private OpenGLCameraSurface openGLCameraSurface;
    private OpenGLCameraRenderer openGLCameraRenderer;

    private short[] recordingBuffer;
    private int[] sharedRecordingOffset;
    private ReentrantLock reentrantLock;
    private ArrayList<String> recognitionLabels;
    private RecordingThread recordingThread;
    private RecognitionThread recognitionThread;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        recordingBuffer = new short[(int) (RecognitionThread.SAMPLE_RATE * RecognitionThread.SAMPLE_DURATION / 1000.0)];
        reentrantLock = new ReentrantLock();
        sharedRecordingOffset = new int[1];

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
        recognitionThread = new RecognitionThread(recordingBuffer, this, reentrantLock, sharedRecordingOffset);

        recordingThread.start();
        recognitionThread.start();
    }

    public void onFrameAvailable(SurfaceTexture surfaceTexture)
    {
        openGLCameraSurface.requestRender();
        //NativeBridge.getInstance().runCameraOperations(camera.getParameters().getPreviewSize().height, camera.getParameters().getPreviewSize().width);
    }

    @Override
    public void onPause()
    {
        //TODO: Does this Kind of Lifecycle Work?
        if(camera != null)
        {
            camera.stopPreview();
            camera.release();
        }
        super.onPause();
    }
}
