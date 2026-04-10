Feature: Routing editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Routing editor
  So that I can create or edit routing data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Routing page
    And I load the Routing editor

  Scenario: Display of the Routing editor
    Then the Routing editor should have 5 separate sections:
      | Routing Details        |
      | Date/Time              |
      | Properties             |
      | Platform Selection     |
      | Platform/Message Types |

  Scenario: Configuring the Routing Details section
    When I configure the information for the following fields:
      | Name                |
      | Data Source         |
      | Destination         |
      | Host/Port           |
      | Output Format       |
      | Time Zone           |
      | Presentation Group  |
    And I set the toggles for the following:
      | In-line computations  |
      | Is Production         |
    Then the information configured in the Routing Details section should persist

  Scenario: Configuring the Date/Time section
    When I configure the information for the following fields:
      | Since    |
      | Until    |
      | Apply To |
    And I check the Ascending Time Order checkbox
    Then the information configured in the Date/Time section should persist

  Scenario: Configuring the Properties table
    When I click on the Plus button in the Properties table
    And a new row is added to the Properties table
    And I configure the content for the Property Name and Value
    Then the entered information for the Property Name and Value should persist

  Scenario: Configuring the Platform Selection section
    When I click the Plus button in the Platform Selection table
    And the Platform Selection editor is loaded
    And I select a row from the Platform Selection editor
    And I click the OK button in the Platform Selection editor
    Then the configured information for the platform selection is displayed in the Platform Selection section

  Scenario: Configuring and successfully saving the updates in the Routing editor
    When I configure the Routing Details section
    And I configure the Properties section
    And I configure the Platform Selection section
    And I click the Save button
    And I receive a confirmation message to save the routing
    And I click OK in the message
    Then I should receive a message that the routing is saved successfully
    And I click on the OK button in the message
    And the new routing should be added to the Routings page
