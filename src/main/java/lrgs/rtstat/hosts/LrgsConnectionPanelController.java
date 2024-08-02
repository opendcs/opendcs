package lrgs.rtstat.hosts;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lrgs.ldds.LddsClient;

public class LrgsConnectionPanelController {

    private LrgsConnectionPanel view;
    private LrgsConnection selectedConnection;
    private LrgsConnectionComboBoxModel model = new LrgsConnectionComboBoxModel(new File(LddsClient.getLddsConnectionsFile()));

    /** Defaults do nothing. */
    private Consumer<LrgsConnection> connectionCallBack = c -> {};
    private BiConsumer<LrgsConnection, LrgsConnection> connectionChangedCallback = (old,c) -> {};

    public void setView(LrgsConnectionPanel lrgsConnectionPanel)
    {
        this.view = lrgsConnectionPanel;
        view.setModel(model);
    }

    public void connect()
    {
        connectionCallBack.accept(selectedConnection);
    }

    public void changeConnection(LrgsConnection c)
    {        
        LrgsConnection oldConnection = selectedConnection;
        selectedConnection = c;
        connectionChangedCallback.accept(oldConnection, c);
        model.addOrReplaceConnection(c);
    }
}
