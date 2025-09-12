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
package decodes.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;

/**
 * Contains the name and filename of a given profile.
 */
public class Profile
{
    private static Logger logger = OpenDcsLoggerFactory.getLogger();

    private final File profile;
    private final String name;
    private final boolean isAProfile;

    /**
     * The default profile will always be "decodes.properties" or "user.properties"
     * Depending on the given configuration and will not change for the life of the programs.
     */
    private final static Profile defaultProfile;

    static
    {
        final String profilePath = EnvExpander.expand("$DCSTOOL_USERDIR");
        final String dcsToolPath = EnvExpander.expand("$DCSTOOL_HOME");
        String defaultName = profilePath.equalsIgnoreCase(dcsToolPath) ? "decodes.properties" : "user.properties";
        File baseDir = new File(profilePath);
        defaultProfile = getProfile(new File(baseDir,defaultName));
    }

    private Profile(File profileFile)
    {
        this.profile = profileFile;
        final String fileName = profile.getName();
        int lastDot = fileName.lastIndexOf(".");
        this.name = fileName.substring(0, lastDot);
        this.isAProfile = fileName.endsWith(".profile");
    }

    public File getFile()
    {
        return profile;
    }

    public String getName()
    {
        return name;
    }

    /**
     * True if this is a named profile, false if one of the default properties files.
     * @return
     */
    public boolean isProfile()
    {
        return isAProfile;
    }

    /**
     * Support default behavior of Profile combo box
     */
    @Override
    public String toString()
    {
        if (name.equals("user") || name.equals("decodes"))
        {
            return "(default)";
        }
        else
        {
            return getName();
        }
    }

    @Override
    public boolean equals(Object other)
    {
        boolean retVal = false;
        if (other instanceof Profile)
        {
            Profile p = (Profile)other;
            if (p.getFile().equals(this.getFile()))
            {
                retVal = true;
            }
        }
        return retVal;
    }

    /**
     * Get a specific profile setup for a file.
     * @param file
     * @return
     */
    public static Profile getProfile(File file)
    {
        return new Profile(file);
    }

    /**
     * Get the default profile based on the environment setup.
     * @return
     */
    public static Profile getDefaultProfile()
    {
        return defaultProfile;
    }

    /**
     * Retrieves all profiles for a given directory.
     * @param directory
     * @return
     */
    public static List<Profile> getProfiles(File directory)
    {
        List<Profile> profiles = new ArrayList<>();
        profiles.add(defaultProfile);
        if (directory.exists() )
        {
            File[] files = directory.listFiles((f) -> f.getAbsolutePath()
                                                       .endsWith(".profile"));
            for (File f: files)
            {
                profiles.add(getProfile(f));
            }
        }
        else
        {
            logger.warn("No profiles, or not a directory, in path {}", directory.getAbsolutePath());
        }
        return profiles;
    }
}
