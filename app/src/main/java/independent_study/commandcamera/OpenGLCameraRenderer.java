package independent_study.commandcamera;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

/**
 * Created by Blaine Huey on 3/5/2018.
 */

public class OpenGLCameraRenderer implements GLSurfaceView.Renderer
{
    private SurfaceTexture surfaceTexture;
    private OpenGLCameraDrawing cameraDrawing;
    private CameraActivity activity;
    private int texture;

    static public int loadShader(int type, String shaderCode)
    {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    static private int createTexture()
    {
        int[] texture = new int[1];

        GLES20.glGenTextures(1,texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    OpenGLCameraRenderer(CameraActivity listenerActivity)
    {
        activity = listenerActivity;
    }

    public void onSurfaceCreated(GL10 gl10, EGLConfig config)
    {
        texture = createTexture();
        cameraDrawing = new OpenGLCameraDrawing(texture);
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
        activity.startCamera(texture);
    }

    public void onDrawFrame(GL10 gl10)
    {
        float[] mtx = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(mtx);

        cameraDrawing.draw();
    }

    public void onSurfaceChanged(GL10 gl10, int width, int height)
    {
        GLES20.glViewport(0, 0, width, height);
    }

    public void setSurface(SurfaceTexture surface)
    {
        surfaceTexture = surface;
    }
}
