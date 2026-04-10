Feature: Schedule Entry editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Schedule Entry editor
  So that I can create or edit schedule entry data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Schedule Entry page
    And I load the Schedule Entry editor

  Scenario: Display of the Schedule Entry editor
    Then there should be an Enabled toggle
    And there shoudl be a Schedule Entry Name field
    And there should be a Loading Application dropdown
    And there should be a Routing Spec dropdown
    And there should be an Execution Schedule section with the following radio button options:
      | Run Continuously |
      | Run Once         |
      | Run Every        |

  Scenario: Configuring and successfully saving the updates in the Schedule Entry editor
    When I set the Enabled toggle
    And I enter information in the Schedule Entry Name field
    And I select an option in the Loading Application dropdown
    And I select an option in the Routing Spec dropdown
    And I select an option in the Execution Schedule section
    And I click the Save button
    And I receive a confirmation message to save the schedule entry
    And I click OK in the message
    Then I should receive a message that the schedule entry is saved successfully
    And I click on the OK button in the message
    And the new schedule entry should be added to the Schedule Entrys page
