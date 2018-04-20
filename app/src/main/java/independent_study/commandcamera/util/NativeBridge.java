package independent_study.commandcamera.util;

/**
 * Class for Interfacing With Native Code
 * Not Using Any, Although I had Intended to Use This For Compression
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
