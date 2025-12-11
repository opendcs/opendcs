Feature: Schedule Entry Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Schedule Entry page
  So that I can create, retrieve, update and delete schedule entry data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Schedule Entry page

  Scenario: Display of the Schedule Entry page
    Then the Schedule Entry page should have the following 6 columns:
      | Name                |
      | Loading Application |
      | Routing Spec        |
      | Enabled?            |
      | Last Modified       |
      | Actions             |

  Scenario Outline: Sorting rows in the Schedule Entry page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Schedule Entry page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Schedule Entry page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column       |
      | Name                |
      | Loading Application |
      | Routing Spec        |
      | Enabled?            |
      | Last Modified       |

  Scenario Outline: Changing the number of entries displayed on a page
    When I click on the Schedule Entry page
    And I change the <Number_of_Entries> to show
    Then there should be <Number_of_Entries> rows displayed

    Examples:
      | Number_of_Entries |
      | 10                |
      | 25                |
      | 50                |
      | 100               |
      | All               |

  Scenario: Access to the Schedule Entry editor by clicking on the Plus button.
    When I click on the Plus button
    Then the Schedule Entry editor should be loaded

  Scenario: Creating a new schedule entry
    When I access the Schedule Entry editor
    And I configure the information needed in the Schedule Entry editor
    And I click Save in the Schedule Entry editor
    And I receive a confirmation message to save the schedule entry
    And I click Yes in the message
    Then I should receive a message that the schedule entry is saved successfully
    And I click on the OK button in the message
    And the new schedule entry should be added to the Schedule Entry page

  Scenario: Access the Schedule Entry editor for a schedule entry
    When I click on an existing row in the Schedule Entry page
    Then the Schedule Entry editor should be loaded with the information for the selected row

  Scenario: Editing the information for a schedule entry
    When I click on an existing row in the Schedule Entry page
    And the Schedule Entry editor should be loaded with the information for the selected row
    And I edit the information in the Schedule Entry editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully

  Scenario: Copying an existing entry
    When I select the Copy option from a row's Actions button
    And the Schedule Entry editor is loaded
    And I edit the information in the Schedule Entry editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully
    And I click OK in the message
    And the new row should be added to the Schedule Entry page

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the schedule entry
    And I click the Yes button in the confirmation message
    Then I should receive a message that the schedule entry is successfully deleted
    And I click OK in the message
    And the schedule entry should be removed from the Schedule Entry page