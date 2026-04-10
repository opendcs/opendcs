Feature: Netlists Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Netlists page
  So that I can create, retrieve, update and delete netlist data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Netlists page

  Scenario: Display of the Netlists page
    Then the Netlists page should have the following 4 columns:
      | List Name      |
      | Medium Type    |
      | # of Platforms |
      | Actions        |

  Scenario Outline: Sorting rows in the Netlists page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Netlists page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Netlists page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column         |
      | List Name      |
      | Medium Type    |
      | # of Platforms |

  Scenario Outline: Changing the number of entries displayed on a page
    When I click on the Netlists page
    And I change the <Number_of_Entries> to show
    Then there should be <Number_of_Entries> rows displayed

    Examples:
      | Number_of_Entries |
      | 10                |
      | 25                |
      | 50                |
      | 100               |
      | All               |

  Scenario: Access to the Netlists editor by clicking on the Plus button.
    When I click on the Plus button
    Then the Netlists editor should be loaded

  Scenario: Creating a new netlist
    When I access the Netlists editor
    And I configure the information needed in the Netlists editor
    And I click Save in the Netlists editor
    And I receive a confirmation message to save the netlist
    And I click Yes in the message
    Then I should receive a message that the netlist is saved successfully
    And I click on the OK button in the message
    And the new netlist should be added to the Netlists page

  Scenario: Access the Netlists editor for a netlist
    When I click on an existing row in the Netlists page
    Then the Netlists editor should be loaded with the information for the selected row

  Scenario: Editing the information for a netlist
    When I click on an existing row in the Netlists page
    And the Netlists editor should be loaded with the information for the selected row
    And I edit the information in the Netlists editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully

  Scenario: Copying an existing entry
    When I select the Copy option from a row's Actions button
    And the Netlists editor is loaded
    And I edit the information in the Netlists editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully
    And I click OK in the message
    And the new row should be added to the Netlists page

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the netlist
    And I click the Yes button in the confirmation message
    Then I should receive a message that the netlist is successfully deleted
    And I click OK in the message
    And the netlist should be removed from the Netlists page