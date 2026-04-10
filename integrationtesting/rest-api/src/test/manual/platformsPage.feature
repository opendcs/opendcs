Feature: Platforms Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Platforms page
  So that I can create, retrieve, update and delete platform data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Platforms page

  Scenario: Display of the Platforms page
    Then the Platforms page should have the following 7 columns:
      | Platform     |
      | Agency       |
      | Transport-ID |
      | Config       |
      | Expiration   |
      | Description  |
      | Actions      |

  Scenario Outline: Sorting rows in the Platforms page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Platforms page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Platforms page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column       |
      | Platform     |
      | Agency       |
      | Transport-ID |
      | Config       |
      | Expiration   |
      | Description  |

  Scenario Outline: Changing the number of entries displayed on a page
    When I click on the Platforms page
    And I change the <Number_of_Entries> to show
    Then there should be <Number_of_Entries> rows displayed

    Examples:
      | Number_of_Entries |
      | 10                |
      | 25                |
      | 50                |
      | 100               |
      | All               |

  Scenario: Access to the Platforms editor by clicking on the Plus button.
    When I click on the Plus button
    Then the Platforms editor should be loaded

  Scenario: Creating a new platform
    When I access the Platforms editor
    And I configure the information needed in the Platforms editor
    And I click Save in the Platforms editor
    And I receive a confirmation message to save the platform
    And I click Yes in the message
    Then I should receive a message that my platform is saved successfully
    And I click on the OK button in the message
    And the new platform should be added to the Platforms page

  Scenario: Access the Platforms editor for a platform
    When I click on an existing row in the Platforms page
    Then the Platforms editor should be loaded with the information for the selected row

  Scenario: Editing the information for a platform
    When I click on an existing row in the Platforms page
    And the Platforms editor should be loaded with the information for the selected row
    And I edit the information in the Platforms editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully

  Scenario: Copying an existing entry
    When I select the Copy option from a row's Actions button
    And the Platforms editor is loaded
    And I edit the information in the Platforms editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully
    And I click OK in the message
    And the new row should be added to the Platforms page

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the platform
    And I click the Yes button in the confirmation message
    Then I should receive a message that the platform is successfully deleted
    And I click OK in the message
    And the platform should be removed from the Platforms page