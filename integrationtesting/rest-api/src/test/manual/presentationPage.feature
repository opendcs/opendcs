Feature: Presentation Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Presentation page
  So that I can create, retrieve, update and delete presentation data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Presentation page

  Scenario: Display of the Presentation page
    Then the Presentation page should have the following 5 columns:
      | Name          |
      | Inherits From |
      | Last Modified |
      | Is Production |
      | Expiration    |
      | Description   |
      | Actions       |

  Scenario Outline: Sorting rows in the Presentation page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Presentation page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Presentation page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column       |
      | Name          |
      | Inherits From |
      | Last Modified |
      | Is Production |
      | Expiration    |
      | Description   |

  Scenario Outline: Changing the number of entries displayed on a page
    When I click on the Presentation page
    And I change the <Number_of_Entries> to show
    Then there should be <Number_of_Entries> rows displayed

    Examples:
      | Number_of_Entries |
      | 10                |
      | 25                |
      | 50                |
      | 100               |
      | All               |

  Scenario: Access to the Presentation Element editor by clicking on the Plus button.
    When I click on the Plus button
    Then the Presentation Element editor should be loaded

  Scenario: Creating a new presentation
    When I access the Presentation editor
    And I configure the information needed in the Presentation editor
    And I click Save in the Presentation editor
    And I receive a confirmation message to save the presentation
    And I click Yes in the message
    Then I should receive a message that the presentation is saved successfully
    And I click on the OK button in the message
    And the new presentation should be added to the Presentation page

  Scenario: Access the Presentation editor for a presentation
    When I click on an existing row in the Presentation page
    Then the Presentation editor should be loaded with the information for the selected row

  Scenario: Editing the information for a presentation
    When I click on an existing row in the Presentation page
    And the Presentation editor should be loaded with the information for the selected row
    And I edit the information in the Presentation editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully

  Scenario: Copying an existing entry
    When I select the Copy option from a row's Actions button
    And the Presentation editor is loaded
    And I edit the information in the Presentation editor
    And I click the Save button
    Then I should receive a message that the edits are saved successfully
    And I click OK in the message
    And the new row should be added to the Presentation page

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the presentation
    And I click the Yes button in the confirmation message
    Then I should receive a message that the presentation is successfully deleted
    And I click OK in the message
    And the presentation should be removed from the Presentation page