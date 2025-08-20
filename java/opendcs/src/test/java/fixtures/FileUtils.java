package fixtures;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Tools to help with writing files for tests.
 */
public class FileUtils
{
    public static void writeFile(File file, String data) throws IOException
    {
        try(FileWriter fw = new FileWriter(file,false))
        {
            fw.write(data);
        }
    }
}
