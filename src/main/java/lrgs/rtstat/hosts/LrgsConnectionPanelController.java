package lrgs.rtstat.hosts;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lrgs.ldds.LddsClient;

public class LrgsConnectionPanelController
{
    private static final Logger log = LoggerFactory.getLogger(LrgsConnectionPanelController.class);
    private LrgsConnectionPanel view;
    private LrgsConnection selectedConnection = LrgsConnection.BLANK;
    private LrgsConnectionComboBoxModel model = new LrgsConnectionComboBoxModel(new File(LddsClient.getLddsConnectionsFile()));

    /** Defaults do nothing. */
    private Function<LrgsConnection,Boolean> connectionCallBack = c -> {return false;};
    private BiConsumer<LrgsConnection, LrgsConnection> connectionChangedCallback = (old,c) -> {};
    private Consumer<Boolean> pauseCallBack = c -> {};

    public void setView(LrgsConnectionPanel lrgsConnectionPanel)
    {
        this.view = lrgsConnectionPanel;
        view.setModel(model);
        model.setSelectedItem(LrgsConnection.BLANK);
    }

    public boolean connect(LrgsConnection c)
    {
        return connectionCallBack.apply(c);
    }

    public void changeConnection(LrgsConnection c)
    {
        LrgsConnection oldConnection = selectedConnection;
        selectedConnection = c;
        model.setSelectedItem(c);
        connectionChangedCallback.accept(oldConnection, c);
    }

    public void onConnect(Function<LrgsConnection,Boolean> connectCallback)
    {
        this.connectionCallBack = connectCallback;
    }

    public void onPause(Consumer<Boolean> onPause)
    {
        this.pauseCallBack = onPause;
    }

    public void pause(boolean paused)
    {
        this.pauseCallBack.accept(paused);
    }
}
