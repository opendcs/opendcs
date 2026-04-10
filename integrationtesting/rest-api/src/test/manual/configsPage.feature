Feature: Configs Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Configs page
  So that I can create, retrieve, update and delete config data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Configs page

  Scenario: Display of the Configs page
    Then the Configs page should have the following 5 columns:
      | Name         |
      | Equipment ID |
      | # Platforms  |
      | Description  |
      | Action       |

  Scenario Outline: Sorting rows in the Configs page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Configs page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Configs page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column       |
      | Name         |
      | Equipment ID |
      | # Platforms  |
      | Description  |

  Scenario Outline: Changing the number of entries displayed on a page
    When I click on the Configs page
    And I change the <Number_of_Entries> to show
    Then there should be <Number_of_Entries> rows displayed

    Examples:
      | Number_of_Entries |
      | 10                |
      | 25                |
      | 50                |
      | 100               |
      | All               |

  Scenario: Access to the Configs editor by clicking on the Plus button
    When I click on the Plus button
    Then the Configs editor should be loaded

  Scenario: Creating a new config
    When I access the Configs editor
    And I configure the information needed in the Configs editor
    And I click Save in the Configs editor
    And I receive a confirmation message to save the config
    And I click Yes in the message
    Then I should receive a message that my config is saved successfully
    And I click on the OK button in the message
    And the new config should be added to the Configs page

  Scenario: Access the Configs editor for a config
    When I click on an existing row in the Configs page
    Then the Configs editor should be loaded with the information for the selected row

  Scenario: Editing the information for a config
    When I click on an existing row in the Configs page
    And the Configs editor should be loaded with the information for the selected row
    And I edit the information in the Configs editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully

  Scenario: Copying an existing entry
    When I select the Copy option from a row's Actions button
    And the Configs editor is loaded
    And I edit the information in the Configs editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully
    And I click OK in the message
    And the new row should be added to the Configs page

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the config
    And I click the Yes button in the confirmation message
    Then I should receive a message that the config is successfully deleted
    And I click OK in the message
    And the config should be removed from the Configs page