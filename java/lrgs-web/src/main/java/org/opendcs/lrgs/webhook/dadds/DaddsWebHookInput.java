/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
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
package org.opendcs.lrgs.webhook.dadds;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.opendcs.lrgs.dao.MsgArchive;

import lrgs.lrgsmain.LoadableLrgsInputInterface;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsInputInterface;

public class DaddsWebHookInput implements LoadableLrgsInputInterface
{
    private final String hookId;
    private final String hookIdHash; // we don't want to render the actual ID
    private boolean enabled;
    private int slot;

    public DaddsWebHookInput(String hookId)
    {
        this.hookId = hookId;
        this.enabled = true;
        try
        {
            this.hookIdHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(hookId.getBytes()));
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new UnsupportedOperationException("Unable to create hash of Hook Id", ex);
        }
    }

    @Override
    public void enableLrgsInput(boolean enable)
    {
        this.enabled = enable;
    }

    @Override
    public String getBER()
    {
        throw new UnsupportedOperationException("Unimplemented method 'getBER'");
    }

    @Override
    public int getDataSourceId()
    {
        return 2001;
    }

    @Override
    public String getGroup()
    {
        return null;
    }

    @Override
    public String getInputName()
    {
        return "WebHook:dadds:" + hookIdHash;
    }

    @Override
    public int getSlot()
    {
        return slot;
    }

    @Override
    public String getStatus()
    {
        return enabled ? "Active" : "Disabled";
    }

    @Override
    public int getStatusCode()
    {
        return enabled ? LrgsInputInterface.DL_ACTIVE : LrgsInputInterface.DL_DISABLED;
    }

    @Override
    public int getType()
    {
        return getDataSourceId();
    }

    @Override
    public boolean getsAPRMessages()
    {
        return false;
    }

    @Override
    public boolean hasBER()
    {
        return false;
    }

    @Override
    public boolean hasSequenceNums()
    {
        return false;
    }

    @Override
    public void initLrgsInput() throws LrgsInputException
    {
        /* nothing to do */
    }

    @Override
    public void setSlot(int slot)
    {
        this.slot = slot;
    }

    @Override
    public void shutdownLrgsInput()
    {
        this.enabled = false;
    }

    @Override
    public void setConfigParam(String name, String value)
    {
        /* not parameters yet */
    }

    @Override
    public void setInterfaceName(String ignored)
    {
        /* no nothing */
    }

    @Override
    public void setMsgArchive(MsgArchive ignored)
    {

        throw new UnsupportedOperationException("Unimplemented method 'setMsgArchive'");
    }

    public String getHookId()
    {
        return this.hookId;
    }
}
