Feature: Enumerations Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Enumerations page
  So that I can create, retrieve, update and delete enumeration data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Enumerations page

  Scenario: Display of the Enumerations page
    Then the Enumerations page should have the following 6 columns:
      | Default               |
      | Name                  |
      | Description           |
      | Java Class (optional) |
      | Options               |
      | Actions               |

  Scenario: Creating a new enumeration
    When I click the Plus button
    And a new row is added to the Enumerations page
    And I enter information for the new enumeration
    And I click the Save button
    And I receive a confirmation message to save the enumeration
    And I click Yes in the message
    Then I should receive a message that the enumeration is saved successfully
    And I click on the OK button in the message
    And the new enumeration should persist in the Enumerations page

  Scenario: Editing the information for a enumeration
    When I click on an existing row in the Enumerations page
    And I edit the information for the enumeration
    And I click the Save button
    And I receive a confirmation message to save the enumeration
    And I click Yes in the message
    Then I should receive a message that the enumeration is saved successfully

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the enumeration
    And I click the Yes button in the confirmation message
    Then I should receive a message that the enumeration is successfully deleted
    And I click OK in the message
    And the enumeration should be removed from the Enumerations page