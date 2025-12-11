Feature: Processes Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Processes page
  So that I can create, retrieve, update and delete process data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Processes page

  Scenario: Display of the Processes page
    Then the Processes page should have the following 5 columns:
      | ID        |
      | Name      |
      | Num Comps |
      | Comment   |
      | Actions   |

  Scenario Outline: Sorting rows in the Processes page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Processes page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Processes page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column      |
      | ID        |
      | Name      |
      | Num Comps |
      | Comment   |

  Scenario Outline: Changing the number of entries displayed on a page
    When I click on the Processes page
    And I change the <Number_of_Entries> to show
    Then there should be <Number_of_Entries> rows displayed

    Examples:
      | Number_of_Entries |
      | 10                |
      | 25                |
      | 50                |
      | 100               |
      | All               |

  Scenario: Access to the Process editor by clicking on the Plus button.
    When I click on the Plus button
    Then the Process editor should be loaded

  Scenario: Creating a new process
    When I access the Process editor
    And I configure the information needed in the Process editor
    And I click Save in the Process editor
    And I receive a confirmation message to save the process
    And I click Yes in the message
    Then I should receive a message that the process is saved successfully
    And I click on the OK button in the message
    And the new process should be added to the Processes page

  Scenario: Access the Process editor for a process
    When I click on an existing row in the Processes page
    Then the Process editor should be loaded with the information for the selected row

  Scenario: Editing the information for a process
    When I click on an existing row in the Processes page
    And the Process editor should be loaded with the information for the selected row
    And I edit the information in the Process editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully

  Scenario: Copying an existing entry
    When I select the Copy option from a row's Actions button
    And the Process editor is loaded
    And I edit the information in the Process editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully
    And I click OK in the message
    And the new row should be added to the Processes page

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the process
    And I click the Yes button in the confirmation message
    Then I should receive a message that the process is successfully deleted
    And I click OK in the message
    And the process should be removed from the Processes page