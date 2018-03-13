#include <jni.h>
#include <GLES2/gl2.h>

#include <android/log.h>
#include <string>

//JNIEXPORT jstring
//JNIEnv *env

extern "C" void JNICALL Java_independent_1study_commandcamera_NativeBridge_runCameraOperations(JNIEnv *env, jint height, jint width)
{
    /*
    const int numPixels = height * width * 4;
    __android_log_print(ANDROID_LOG_DEBUG, "JNI", "Start");
    static char pixels[numPixels];
    __android_log_print(ANDROID_LOG_DEBUG, "JNI", "CreatedPixels");
    glReadPixels(0, 0, height, width, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
    __android_log_print(ANDROID_LOG_DEBUG, "JNI", "Read %d", pixels[0]);
    */
}