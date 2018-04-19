package independent_study.commandcamera;

import android.content.Context;
import android.opengl.GLSurfaceView;

/**
 * Simple Android GLSurfaceView that Utilizes GLCameraRenderer
 */
public class OpenGLCameraSurface extends GLSurfaceView
{
    private OpenGLCameraRenderer renderer;

    /**
     * Sets Up View Displaying Rendered Camera Info
     * @param context - Android Context (Required for super)
     */
    public OpenGLCameraSurface(Context context)
    {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new OpenGLCameraRenderer((CameraActivity) context);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /**
     * Returns the Renderer State Variable for Use by Activity
     * @return the Renderer State Variable for Use by Activity
     */
    public OpenGLCameraRenderer getRenderer()
    {
        return renderer;
    }
}
