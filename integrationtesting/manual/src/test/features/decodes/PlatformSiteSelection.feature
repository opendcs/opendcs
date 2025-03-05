@Kiwi.Plan(DbEdit)
@Kiwi.Priority(Medium)
Feature: Platform Site selection

    Background: Basic Setup
        Given A database with some platforms available and several sites.

    Scenario Outline: Opening by <open method>

        Given There are platforms in the list
        When The user selects a Platform and opendcs it by <open method>
        Then The Platform editing window is opened

        When The user clicks "Choose" to select a site
        Then The site selection window is opened

        When The user orders the list by any column, selects the row, and clicks okay
        Then The selected site is shown in the "Site" text box.

    Examples:
        | open method            |
        |Select and clicking Open|
        |Double-Click the row    |
