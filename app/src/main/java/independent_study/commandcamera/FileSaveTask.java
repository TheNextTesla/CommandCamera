package independent_study.commandcamera;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_PICTURES;

public class FileSaveTask extends AsyncTask<Void, Void, Void>
{
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss", Locale.US);
    private static final Date date = new Date();
    private static final String folder = "CommandCamera";
    private static final String fileStart = "CC-";
    private static final String fileEnding = ".jpg";

    private static File directoryPictures = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES);
    private static File directoryCommandCamera = new File(directoryPictures, folder);

    private byte[] bytes;

    public FileSaveTask(byte[] bytes)
    {
        this.bytes = bytes;
    }

    public Void doInBackground(Void... voids)
    {
        try
        {
            if(directoryCommandCamera == null || directoryPictures == null)
            {
                Log.d("FileSaveTask", "Directory DNE");
                return null;
            }

            if(directoryPictures.isDirectory() && directoryPictures.exists())
            {
                if(!directoryCommandCamera.exists() || !directoryCommandCamera.isDirectory())
                {
                    if(directoryCommandCamera.mkdir())
                    {
                        Log.d("FileSaveTask", "Folder Created at " + directoryCommandCamera.getAbsolutePath());
                    }
                }
            }

            date.setTime(System.currentTimeMillis());
            String fileName = fileStart + sdf.format(date);
            File file = File.createTempFile(fileName, fileEnding, directoryCommandCamera);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return null;
    }
}
