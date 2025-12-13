Feature: Computations Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Computations page
  So that I can create, retrieve, update and delete computation data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Computations page

  Scenario: Display of the Computations page
    Then the Computations page should have the following 7 columns:
      | ID        |
      | Name      |
      | Algorithm |
      | Process   |
      | Enabled   |
      | Comment   |
      | Actions   |

  Scenario Outline: Sorting rows in the Computations page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Computations page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Computations page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column    |
      | ID        |
      | Name      |
      | Algorithm |
      | Process   |
      | Enabled   |
      | Comment   |

  Scenario Outline: Changing the number of entries displayed on a page
    When I click on the Computations page
    And I change the <Number_of_Entries> to show
    Then there should be <Number_of_Entries> rows displayed

    Examples:
      | Number_of_Entries |
      | 10                |
      | 25                |
      | 50                |
      | 100               |
      | All               |

  Scenario: Access to the Computations editor by clicking on the Plus button.
    When I click on the Plus button
    Then the Computations editor should be loaded

  Scenario: Creating a new computation
    When I access the Computations editor
    And I configure the information needed in the Computations editor
    And I click Save in the Computations editor
    And I receive a confirmation message to save the computation
    And I click Yes in the message
    Then I should receive a message that the computation is saved successfully
    And I click on the OK button in the message
    And the new computation should be added to the Computations page

  Scenario: Access the Computations editor for a computation
    When I click on an existing row in the Computations page
    Then the Computations editor should be loaded with the information for the selected row

  Scenario: Editing the information for a computation
    When I click on an existing row in the Computations page
    And the Computations editor should be loaded with the information for the selected row
    And I edit the information in the Computations editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully

  Scenario: Copying an existing entry
    When I select the Copy option from a row's Actions button
    And the Computations editor is loaded
    And I edit the information in the Computations editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully
    And I click OK in the message
    And the new row should be added to the Computations page

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the computation
    And I click the Yes button in the confirmation message
    Then I should receive a message that the computation is successfully deleted
    And I click OK in the message
    And the computation should be removed from the Computations page