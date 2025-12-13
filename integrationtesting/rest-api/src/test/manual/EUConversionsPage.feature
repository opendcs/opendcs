Feature: EU Conversions Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the EU Conversions page
  So that I can create, retrieve, update and delete conversion data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the EU Conversions page

  Scenario: Display of the EU Conversions page
    Then the EU Conversions page should have the following 10 columns:
      | From      |
      | To        |
      | Algorithm |
      | A         |
      | B         |
      | C         |
      | D         |
      | E         |
      | F         |
      | Actions   |

  Scenario Outline: Sorting rows in the EU Conversions page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the EU Conversions page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the EU Conversions page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column      |
      | From      |
      | To        |
      | Algorithm |
      | A         |
      | B         |
      | C         |
      | D         |
      | E         |
      | F         |

  Scenario: Creating a new conversion
    When I click the Plus button
    And a new row is added to the EU Conversions page
    And I enter information for the new conversion
    And I click the Save button
    And I receive a confirmation message to save the conversion
    And I click Yes in the message
    Then I should receive a message that the conversion is saved successfully
    And I click on the OK button in the message
    And the new conversion should persist in the EU Conversions page

  Scenario: Editing the information for a conversion
    When I click on an existing row in the EU Conversions page
    And I edit the information for the conversion
    And I click the Save button
    And I receive a confirmation message to save the conversion
    And I click Yes in the message
    Then I should receive a message that the conversion is saved successfully

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the conversion
    And I click the Yes button in the confirmation message
    Then I should receive a message that the conversion is successfully deleted
    And I click OK in the message
    And the conversion should be removed from the EU Conversions page