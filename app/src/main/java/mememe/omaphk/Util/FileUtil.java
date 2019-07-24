package mememe.omaphk.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Paul Wong on 17/04/19.
 */

public class FileUtil {

    static public Boolean createFileFromInputStream(InputStream inputStream, File target) {

        try{
            if(!target.getParentFile().exists()) target.getParentFile().mkdirs();

            OutputStream outputStream = new FileOutputStream(target);
            byte[] buffer = new byte[1024];
            int length = 0;

            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }

            outputStream.close();
            inputStream.close();

            return true;
        }catch (Exception e) {
            e.printStackTrace();
            //Logging exception
            return false;
        }
    }

}
