**Scenario** Behavior Launcher with no profiles.
  
**Given** 
a $DCSTOOL_USERDIR with a user.properties and no .profile files

**When**

1. The user starts the launcher with no profile.
2. The user clicks setup

**Then**
1. There is no profile combo box.
2. The Property editor is opened with data from the user.properties file
# TODO: Add specific search text when files are generated for the testers
