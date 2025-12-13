Feature: Sites Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Sites page
  So that I can create, retrieve, update and delete site data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Sites page

  Scenario: Display of the Sites page
    Then the Sites page should have a Displayed Type dropdown
    And the Sites page should have the following 4 columns:
      | Site Name             |
      | Configured Site Names |
      | Description           |
      | Actions               |

  Scenario Outline: Sorting rows in the Sites page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Sites page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Sites page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column                |
      | Site Name             |
      | Configured Site Names |
      | Description           |

  Scenario Outline: Changing the number of entries displayed on a page
    When I click on the Sites page
    And I change the <Number_of_Entries> to show
    Then there should be <Number_of_Entries> rows displayed

    Examples:
      | Number_of_Entries |
      | 10                |
      | 25                |
      | 50                |
      | 100               |
      | All               |

  Scenario Outline: Updating the displayed content after switching the Displayed Type
    When I select the <Type> option in the Dispalyed Type dropdown
    Then the <Type> should be displayed next tot he Site Name column header
    And the content should be filtered to configured site names with the selected <Type>

    Examples:
      | Type      |
      | cbtt      |
      | nwdpid    |
      | cwms      |
      | nwshb5    |
      | usgs-drgs |
      | usgs      |
      | local     |

  Scenario: Access to the Sites editor by clicking on the Plus button.
    When I click on the Plus button
    Then the Sites editor should be loaded

  Scenario: Creating a new site
    When I access the Sites editor
    And I configure the information needed in the Sites editor
    And I click Save in the Sites editor
    And I receive a confirmation message to save the site
    And I click Yes in the confirmation message
    Then I should receive a message that the site is saved successfully
    And I click on the OK button in the message
    And the new site should be added to the Sites page

  Scenario: Access the Sites editor for a site
    When I click on an existing row in the Sites page
    Then the Sites editor should be loaded with the information for the selected row

  Scenario: Editing the information for a site
    When I click on an existing row in the Sites page
    And the Sites editor should be loaded with the information for the selected row
    And I edit the information in the Sites editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the site
    And I click the Yes button in the confirmation message
    Then I should receive a message that the site is successfully deleted
    And I click OK in the message
    And the platform should be removed from the Sites page