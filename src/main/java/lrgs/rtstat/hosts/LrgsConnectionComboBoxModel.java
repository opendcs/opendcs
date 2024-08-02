package lrgs.rtstat.hosts;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LrgsConnectionComboBoxModel extends DefaultComboBoxModel<LrgsConnection>
{
    private static final Logger log = LoggerFactory.getLogger(LrgsConnectionComboBoxModel.class);

    final List<LrgsConnection> hosts = new ArrayList<>();
    final File lddsConnectionFile;

    public LrgsConnectionComboBoxModel(File lddsConnectionFile)
    {
        this.lddsConnectionFile = lddsConnectionFile;
        hosts.add(LrgsConnection.BLANK);
        try (FileInputStream fis = new FileInputStream(lddsConnectionFile);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis)))
        {
            String line;
            while ( (line = br.readLine()) != null)
            {
                if (!(line.startsWith("#") || !line.trim().isEmpty()))
                {
                    System.out.println(line);
                    hosts.add(LrgsConnection.fromDdsFile(line));
                }
            }
		}
		catch(IOException ioe)
		{
			log.error("No previously recorded connections");
		}
		
    }

    public void addOrReplaceConnection(LrgsConnection c)
    {
        synchronized (hosts)
        {
            for (int i = 0; i < hosts.size(); i++)
            {
                LrgsConnection host = hosts.get(i);
                if (host.hostName.equals(c.hostName) && !host.equals(c))
                {
                    hosts.set(i, c);
                    fireContentsChanged(c, i, i);
                    writeToDisk();
                    return;
                }
            }
            hosts.add(c);
            fireIntervalAdded(c, getSize(), getSize());
            writeToDisk();
        }
    }

    private void writeToDisk()
    {
        try(FileOutputStream fos = new FileOutputStream(lddsConnectionFile, false);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));)
        {
            bw.write("#Recent LRGS Connections"+System.lineSeparator());
            bw.write(String.format("#%s%s", new Date().toString(), System.lineSeparator()));
            for (LrgsConnection c: hosts)
            {
                if (c != LrgsConnection.BLANK)
                {
                    bw.write(c.toString());
                    bw.write(System.lineSeparator());
                }    
            }
        }
        catch (IOException ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to write LddsConnections to {}", lddsConnectionFile);
        }
    }
}
