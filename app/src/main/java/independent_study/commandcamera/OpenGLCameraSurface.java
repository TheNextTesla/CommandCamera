package independent_study.commandcamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Camera;
import android.opengl.GLSurfaceView;

/**
 * Created by Blaine Huey on 3/2/2018.
 */

public class OpenGLCameraSurface extends GLSurfaceView
{
    private OpenGLCameraRenderer renderer;

    public OpenGLCameraSurface(Context context)
    {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new OpenGLCameraRenderer((CameraActivity) context);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public OpenGLCameraRenderer getRenderer()
    {
        return renderer;
    }
}
