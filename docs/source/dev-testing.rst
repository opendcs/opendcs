#######################
Manual Testing Guidance
#######################

This Document is part of the OpenDCS Software Suite for environmental
data acquisition and processing. The project home is:
https://github.com/opendcs/opendcs

See INTENT.md at the project home for information on licensing.

.. contents. Table of Contents
   :depth: 3


Overview
========

This document describes how various testing is done for OpenDCS.
We have Automated and Manual tests.


Automated
=========


TODO: Some documentation is available in the dev docs section.


Manual Testing
==============

Overview
--------

Manual tests are written in the Gherkin (https://cucumber.io/docs/gherkin/) syntax to provide structure to the tests.
The tests can be found in the OpenDCS repository here: github.com/opendcs/opendcs/integrationtesting/manual/src/main/features

We have created a plugin, https://github.com/opendcs/gherkin-to-kiwi, to process these files and push them to a KiwiTCMS (https://kiwitcms.org/) instance.

At this time USACE maintains an internal instance of KiwiTCMS and will be pushing the tests to that. We will primarily be testing CWMS.

It is the goal of the gherkin-to-kiwi project to be more flexible and output to other systems, including simple CSV or fillable PDFs; However, the initial
design focused on KiwiTCMS. The terminology below will use that of KiwiTCMS.

Test organization
-----------------

Tests are organized as "Test Cases" groups by "Test Plans". A Test Case can be part of multiple test plans.

TestsPlans are defined in the build.gradle. 


.. code-block:: groovy

    kiwi {
        product = "OpenDCS"
        
        plans {
            DbEdit {
                planName = "Decodes Database Editor"
                type = "Acceptance"
            }

            Launcher {
                planName = "Application Launcher"
                type = "Acceptance"
            }
        }

        outputs {
            hec {
                product = "OpenDCS"
                type = "kiwi"
                url = project.findProperty("kiwi.url") ?: "url not set"
                version = project.version
                username = project.findProperty("kiwi.user") ?: "username not set"
                password = project.findProperty("kiwi.password") ?: "password not set"
            }
        }
    }


In the above `DbEdit` is an ID field with no spaces that can be referenced in a feature file, described next

.. WARNING:: 

    As this was written it was noticed that the opening section of this configuration block is "kiwi"
    This should be something generic like "tcms" and will be changed in a future release of the plugin.


Test Cases
----------

Tests Cases are the basic unit of testing, similar to a single `@Test` function in junit.

Tests are written in .feature files that that use the Gherkin syntax.


.. code-block:: gherkin

    @Kiwi.Plan(Launcher)
    @Kiwi.Priority(Medium)
    Feature: Launcher Application

    Scenario: Launcher with no profiles, only user.properties
  

        Given $DCSTOOL_USERDIR with a user.properties and no .profile files
         When The User starts the launcher
         Then The Profile combo box is present with one entry

         When The user clicks Setup
         Then The correct properties for the user.properties is rendered.

         When The user clicks the button (...) next to the Profile
         Then The Profile manager panel is opened.

         When the user select the default profile, clicks copy, provides a name and closes the window
         Then the Profile combo contains two entries.

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


The name of the feature references a specific component, in this case the Launcher application.
In KiwiTCMS a component would be created, and added to the test case.

Scenario describes the actual test. The text after `Scenario:` is used as the "summary" in KiwiTCMS.

Given,When,Then setup the preconditions of the test, the steps the tester is to engage in, and the expected results.

Images are not yet supported, but support is planned. 


Workflow
========

Automation will be setup to push tests, plans, and setup for testers to run to the HEC KiwiTCMS system.
While specific to the CWMS Implementation this will still catch most GUI issues as CWMS and the reference 
database (currently named OpenTSDB) share many implementation details.

When time is available to implement support we will output CSV, PDF, or both to the daily builds for interested parties.


Future work
===========

- Images
- Support for other TCMS systems
- Support for implementation variations
- Expand the tests
