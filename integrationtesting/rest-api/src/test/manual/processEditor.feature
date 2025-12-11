Feature: Process Editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Process editor
  So that I can create or edit process data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Processes page
    And I load the Process editor

  Scenario: Display of the Process editor
    Then the Process Editor should have 3 separate sections:
      | Details                |
      | Comments               |
      | Application Properties |

  Scenario: Configuring the Details in the Process editor
    When I enter information in the Process Name field
    And I select an option in the Process Type dropdown
    And I set the Manual Edit App toggle
    Then the information configured in the Details section should persist

  Scenario: Configuring the Application Properties table
    When I click on the Plus button in the Application Properties table
    And a new row is added to the Properties table
    And I configure the content for the Property Name and Value
    Then the entered information for the Property Name and Value should persist

  Scenario: Configuring and successfully saving the updates in the Process editor
    When I configure the Details section
    And I enter information in the Comments section
    And I configure the Application Properties section
    And I click the Save button
    And I receive a confirmation message to save the process
    And I click OK in the message
    Then I should receive a message that my process is saved successfully
    And I click on the OK button in the message
    And the new process should be added to the Processes page
