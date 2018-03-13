package independent_study.commandcamera;

import java.lang.annotation.Native;

/**
 * Created by Blaine Huey on 3/9/2018.
 */

public class NativeBridge
{
    static
    {
        System.loadLibrary("native-lib");
    }

    private static NativeBridge nativeBridge;

    public static NativeBridge getInstance()
    {
        if(nativeBridge == null)
            nativeBridge = new NativeBridge();
        return nativeBridge;
    }

    private NativeBridge()
    {

    }

    public native void runCameraOperations(int height, int width);
}
