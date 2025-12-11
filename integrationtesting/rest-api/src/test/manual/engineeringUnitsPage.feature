Feature: Engineering Units Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Engineering Units page
  So that I can create, retrieve, update and delete unit data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Engineering Units page

  Scenario: Display of the Engineering Units page
    Then the Engineering Units page should have the following 5 columns:
      | Abbrev    |
      | Full Name |
      | Family    |
      | Measures  |
      | Actions   |

  Scenario Outline: Sorting rows in the Engineering Units page
    When I click on the double arrow button on the <Column> columnn
    And the button for the <Column> column changes to an up arrow button
    Then the rows in the Engineering Units page should be sorted in ascending order based on the content in the <Column> column
    When I click on the up arrow button
    Then the rows in the Engineering Units page should be sorted in descending order based on the content in the <Column> column

    Examples:
      | Column      |
      | Abbrev    |
      | Full Name |
      | Family    |
      | Measures  |

  Scenario: Creating a new unit
    When I click the Plus button
    And a new row is added to the Engineering Units page
    And I enter information for the new unit
    And I click the Save button
    And I receive a confirmation message to save the unit
    And I click Yes in the message
    Then I should receive a message that the unit is saved successfully
    And I click on the OK button in the message
    And the new unit should persist in the Engineering Units page

   Scenario: Editing the information for a unit
     When I click on an existing row in the Engineering Units page
     And I edit the information for the unit
     And I click the Save button
     And I receive a confirmation message to save the unit
     And I click Yes in the message
     Then I should receive a message that the unit is saved successfully

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the unit
    And I click the Yes button in the confirmation message
    Then I should receive a message that the unit is successfully deleted
    And I click OK in the message
    And the unit should be removed from the Engineering Units page