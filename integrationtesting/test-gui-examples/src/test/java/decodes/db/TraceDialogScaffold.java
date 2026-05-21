package decodes.db;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.dbeditor.TraceDialog;

public class TraceDialogScaffold
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args)
    {

        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new JButton("I'm a button"));
        frame.pack();

        SwingUtilities.invokeLater(() -> frame.setVisible(true));

        SwingUtilities.invokeLater(() ->
        {
            TraceDialog td = new TraceDialog(frame, false);
            td.setVisible(true);
		});

        executor.scheduleAtFixedRate(() -> log.info("A random message: " + UUID.randomUUID().toString()), 10000, 100, TimeUnit.MILLISECONDS);

    }
}
