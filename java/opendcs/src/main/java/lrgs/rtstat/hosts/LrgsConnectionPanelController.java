/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package lrgs.rtstat.hosts;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import lrgs.ldds.LddsClient;

public class LrgsConnectionPanelController
{
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
