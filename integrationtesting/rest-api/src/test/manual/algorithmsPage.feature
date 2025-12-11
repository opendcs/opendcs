Feature: Algorithms Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Algorithms page
  So that I can create, retrieve, update and delete algorithm data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Algorithms page

  Scenario: Display of the Algorithms page
    Then the Algorithms page should have the following 5 columns:
      | Name       |
      | Exec Class |
      | #Comps     |
      | Comment    |
      | Actions    |

  Scenario Outline: Sorting rows in the Algorithms page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Algorithms page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Algorithms page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column      |
      | Name        |
      | Exec Class  |
      | #Comps      |
      | Comment     |

  Scenario Outline: Changing the number of entries displayed on a page
    When I click on the Algorithms page
    And I change the <Number_of_Entries> to show
    Then there should be <Number_of_Entries> rows displayed

    Examples:
      | Number_of_Entries |
      | 10                |
      | 25                |
      | 50                |
      | 100               |
      | All               |

  Scenario: Access to the Algorithm editor by clicking on the Plus button.
    When I click on the Plus button
    Then the Algorithm editor should be loaded

  Scenario: Creating a new algorithm
    When I access the Algorithm editor
    And I configure the information needed in the Algorithm editor
    And I click Save in the Algorithm editor
    And I receive a confirmation message to save the algorithm
    And I click Yes in the message
    Then I should receive a message that the algorithm is saved successfully
    And I click on the OK button in the message
    And the new algorithm should be added to the Algorithms page

  Scenario: Access the Algorithm editor for a algorithm
    When I click on an existing row in the Algorithms page
    Then the Algorithm editor should be loaded with the information for the selected row

  Scenario: Editing the information for a algorithm
    When I click on an existing row in the Algorithms page
    And the Algorithm editor should be loaded with the information for the selected row
    And I edit the information in the Algorithm editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully

  Scenario: Copying an existing entry
    When I select the Copy option from a row's Actions button
    And the Algorithm editor is loaded
    And I edit the information in the Algorithm editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully
    And I click OK in the message
    And the new row should be added to the Algorithms page

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the algorithm
    And I click the Yes button in the confirmation message
    Then I should receive a message that the algorithm is successfully deleted
    And I click OK in the message
    And the algorithm should be removed from the Algorithms page