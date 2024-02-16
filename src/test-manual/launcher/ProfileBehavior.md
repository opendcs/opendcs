**Scenario** Behavior of profile selection within the Launcher.
  
**Given** 
a $DCSTOOL_USERDIR with a user.properties and at least two profiles

**When**

1. The user starts the launcher with no profile.
2. The user clicks setup

**Then**
1. "(default) is shown as the selected profile.
2. The Property editor is opened with data from the user.properties file
# TODO: Add specific search text when files are generated for the testers

**When**
1. The user starts the launcher with no profile.
2. The user select a different profile.
3. The user clicks Setup.

**Then**
1. The Property editor opens with data from the selected .profile file.

**When**
1. The user starts the launcher with a profile `launcher_start -P <full path to profile>`
2. The user clicks Setup.

**Then**
1. The short name (filename without extension) is shown as the selected profile.
2. The Property Editor loads data from the selected .profile file.

**When**
1. The user select a different profile, including "(default)"
2. The user clicks Setup.

**Then**
1. The combobox updates to show the selected profile.
2. The property editor loads data from the selected .profile (or user.properties) file.
