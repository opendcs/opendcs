Feature: Routing Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Routing page
  So that I can create, retrieve, update and delete routing data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Routing page

  Scenario: Display of the Routing page
    Then the Routing page should have the following 5 columns:
      | Name          |
      | Data Source   |
      | Consumer      |
      | Last Modified |
      | Actions       |

  Scenario Outline: Sorting rows in the Routing page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Routing page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Routing page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column        |
      | Name          |
      | Data Source   |
      | Consumer      |
      | Last Modified |

  Scenario Outline: Changing the number of entries displayed on a page
    When I click on the Routing page
    And I change the <Number_of_Entries> to show
    Then there should be <Number_of_Entries> rows displayed

    Examples:
      | Number_of_Entries |
      | 10                |
      | 25                |
      | 50                |
      | 100               |
      | All               |

  Scenario: Access to the Routing editor by clicking on the Plus button.
    When I click on the Plus button
    Then the Routing editor should be loaded

  Scenario: Creating a new routing
    When I access the Routing editor
    And I configure the information needed in the Routing editor
    And I click Save in the Routing editor
    And I receive a confirmation message to save the routing
    And I click Yes in the message
    Then I should receive a message that my routing is saved successfully
    And I click on the OK button in the message
    And the new sources should be added to the Routing page

  Scenario: Access the Sources editor for a routing
    When I click on an existing row in the Routing page
    Then the Routing editor should be loaded with the information for the selected row

  Scenario: Editing the information for a routing
    When I click on an existing row in the Routing page
    And the Routing editor should be loaded with the information for the selected row
    And I edit the information in the Routing editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully

  Scenario: Copying an existing entry
    When I select the Copy option from a row's Actions button
    And the Routing editor is loaded
    And I edit the information in the Routing editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully
    And I click OK in the message
    And the new row should be added to the Routing page

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the routing
    And I click the Yes button in the confirmation message
    Then I should receive a message that the routing is successfully deleted
    And I click OK in the message
    And the routing should be removed from the Routing page