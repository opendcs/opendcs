@Kiwi.Plan(Launcher)
@Kiwi.Priority(Medium)
Feature: Launcher Application

    Scenario: Launcher with no profiles, only user.properties
  

        Given $DCSTOOL_USERDIR with a user.properties and no .profile files
         When The User starts the launcher
         Then The Profile combo box is present with one entry

         When The user clicks the Setup button
         Then The correct properties for the user.properties is rendered.

         When The user clicks the button (...) next to the Profile
         Then The Profile manager panel is opened.

         When The user select the default profile (in the Profile Manager), clicks copy, provides a name and closes the window
         Then The Profile combo contains two entries.

    Scenario: Launcher with multiple profiles

        Given $DCSTOOL_USERDIR with a user.properties and at least two .profile files
         When The user starts the launcher
         Then The profile combo box contains 3 entries, default, and all the profile file names.

         When The user select a profile and clicks Setup
         Then The correct properties are rendered in the dialog.

    Scenario: Launcher with provided default profile
        Given $DCSTOOL_USERDIR with a user.properties and at least two .profile files
          And the launcher is run with `launcher_start -P profileName.profile`
         When the user looks at the combo box
         Then profileName is not present, user.properties is shown, and the other profile names are present.
