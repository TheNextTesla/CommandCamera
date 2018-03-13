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

import java.io.IOException;

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_KEY);
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
