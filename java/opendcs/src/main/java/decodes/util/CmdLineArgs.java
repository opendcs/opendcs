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
package decodes.util;

import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.gui.TopFrame;
import decodes.launcher.Profile;
import ilex.cmdline.*;
import ilex.util.EnvExpander;

/**
Extends the ilex.cmdline.StdAppSettings class to handle arguments that
are common to most DECODES programs.
*/
public class CmdLineArgs extends StdAppSettings
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    // Add DECODES-specific setting declarations here...

    /** Log file argument (-l) */
    protected StringToken log_arg;
    /** properties file argument (-P), converted to a {@link decodes.launcher.Profile} */
    private Profile profile;
    /** Application Define argument (-D) */
    private StringToken define_arg;
    private BooleanToken forwardLogArg;

    // Used by parent programs that spawn multiple apps. The parent sets this to
    // tell parseArgs to skip the logger and properties initialization.
    private boolean noInit = false;

    //No filter option token.
    public StringToken NoCompFilterToken;

    /** Properties set explicitly on the command line. */
    private Properties cmdLineProps;

    /** default constructor */
    public CmdLineArgs()
    {
        this(true, "util.log");
    }

    /**
      Explicit constructor.
      @param isNetworkApp True if this is a network-aware application
      @param defaultLogFile Initialize log file name
    */
    public CmdLineArgs(boolean isNetworkApp, String defaultLogFile)
    {
        super(isNetworkApp);

        // Construct DECODES-specific setting & call addToken for each one.
        log_arg = new StringToken(
            "l", "log-file", "", TokenOptions.optSwitch, defaultLogFile);
        define_arg = new StringToken(
            "D", "Env-Define", "",
            TokenOptions.optSwitch|TokenOptions.optMultiple, "");
        forwardLogArg = new BooleanToken("FL", "Forward javax.logging logger to application log.", "",
            TokenOptions.optSwitch, false);
        addToken(log_arg);
        addToken(define_arg);
        addToken(forwardLogArg);
        cmdLineProps = new Properties();

    }

    /** @return log file name, either default, or as specified in argumnet */
    public String getLogFile()
    {
        String s = log_arg.getValue();
        if (s == null || s.length() == 0)
            return null;
        return s;
    }

    public void setDefaultLogFile(String f)
    {
        log_arg.setDefaultValue(f);
    }

    /**
      Parses the command line argument and fills in internal variables.
      @param args the arguments.
    */
    public void parseArgs(String args[])
    {
        super.parseArgs(args);

        /*
          The user can set system properties on the command line with multiple
          -Dname=value arguments. The following puts each setting into
          System.properties so that they are available globally.

          Each 'name' is converted to upper case before putting in the
          properties set. So retrieve the property by upper-case name only.
        */
        for(int i=0; i<define_arg.NumberOfValues(); i++)
        {
            String arg = define_arg.getValue(i).trim();
            if (arg == null || arg.length() == 0)
                continue;
            int idx = arg.indexOf('=');
            if (idx == -1 || arg.length() <= idx+1)
            {
                log.error("Invalid define '{}' -- should be in the form name=value.", arg);
            }
            String name = arg.substring(0,idx).trim();
            String value = arg.substring(idx+1).trim();
            if (name.length() == 0)
            {
                log.error("Invalid define name='{}', value='", name, value);
                System.exit(1);
            }
            System.setProperty(name.toUpperCase(), value);
            cmdLineProps.setProperty(name, value);
        }

        if (noInit)
            return;


        DecodesSettings settings = DecodesSettings.instance();

        // if -P arg supplied, use it, else look in install dir.
        final String propFileName = super.getPropertiesFile();
        if (propFileName != null && propFileName.length() > 0)
        {
            profile = Profile.getProfile(new File(EnvExpander.expand(propFileName)));
        }
        else // the default profile is always at index 0 in Profile.getProfiles
        {
            profile = Profile.getDefaultProfile();
        }
        File propFile = profile.getFile();

        if (!propFile.canRead())
        {
            String msg = "Cannot read properties file '" + propFile.getPath()
                + "' -P ARGUMENT PARSING or retrieval of default properties FAILED!";
            throw new IllegalArgumentException(msg);
        }

        if (profile.isProfile())
        {
            String profileName = profile.getName();
            TopFrame.profileName = profileName; // don't include the full path that may be there.

            if (!profileName.equalsIgnoreCase("user") && !profileName.equalsIgnoreCase("decodes"))
            {
                settings.setProfileName(profileName);
            }
        }

        //Load the decodes.properties
        if (!settings.isLoaded())
        {
            try
            {
                settings.loadFromProfile(profile);
            }
            catch(Exception ex)
            {
                log.atError().setCause(ex).log("Cannot load decodes properties from '{}'", propFileName);
            }
        }

        // Userdir is needed to support multi-user installations under unix/linux.
        // If the property is not set, just copy DCSTOOL_HOME.
        // That is, assume this is a single-user or windows installation.
        String userDir = System.getProperty("DCSTOOL_USERDIR");
        if (userDir == null)
        {
            System.setProperty("DCSTOOL_USERDIR", System.getProperty("user.dir")+"/.opendcs");
        }

        if (DecodesSettings.instance().fontAdjust != 0)
        {
            for (Map.Entry<Object, Object> entry : javax.swing.UIManager.getDefaults().entrySet())
            {
                Object key = entry.getKey();
                Object value = javax.swing.UIManager.get(key);
                if (value != null && value instanceof javax.swing.plaf.FontUIResource)
                {
                    javax.swing.plaf.FontUIResource fr=(javax.swing.plaf.FontUIResource)value;
                    javax.swing.plaf.FontUIResource f = new javax.swing.plaf.FontUIResource(fr.getFamily(),
                        fr.getStyle(), fr.getSize() + DecodesSettings.instance().fontAdjust);
                    javax.swing.UIManager.put(key, f);
                }
            }
        }

        log.info("After parseArgs, DecodesSettings src file={}", DecodesSettings.instance().getSourceFile().getPath());
    }

    /**
     * May be the default .properties file wrapped in a Profile object
     * @return DECODES Properties Profile.
     * */
    public Profile getProfile()
    {
        return profile;
    }

    public Properties getCmdLineProps() { return cmdLineProps; }

    public void setNoInit(boolean noInit)
    {
        this.noInit = noInit;
    }

    public boolean isNoInit()
    {
        return noInit;
    }
}