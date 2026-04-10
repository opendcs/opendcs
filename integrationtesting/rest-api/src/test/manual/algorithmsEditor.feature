Feature: Algorithm Editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Algorithm editor
  So that I can create or edit algorithm data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Algorithms page
    And I load the Algorithm editor

  Scenario: Display of the Algorithm editor
    Then the Algorithm Editor should have 3 separate sections:
      | Details    |
      | Parameters |
      | Properties |

  Scenario: Configuring the Details in the Algorithm editor
    When I configure the information for the following fields:
      | Algorithm Name |
      | Exec Class     |
      | Algorithm ID   |
      | Num Comps      |
      | Comments       |
    Then the information configured in the Details section should persist

  Scenario: Configuring the Parameters section
    When I click on the Plus button in the Parameters section
    And a new row is added to the Parameters section
    And I configure the content for the Role Name and Type Code
    Then the entered information for the Role Name and Type Code should persist

  Scenario: Configuring the Properties section
    When I configure the content for the Value
    Then the entered information for the Value should persist

  Scenario: Configuring and successfully saving the updates in the Algorithm editor
    When I configure the Details section
    And I configure the Parameters section
    And I configure the Properties section
    And I click the Save button
    And I receive a confirmation message to save the algorithm
    And I click OK in the message
    Then I should receive a message that the algorithm is saved successfully
    And I click on the OK button in the message
    And the new algorithm should be added to the Algorithms page
