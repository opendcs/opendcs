Feature: Computations Editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Computations editor
  So that I can create or edit computation data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Computations page
    And I load the Computations editor

  Scenario: Display of the Computations editor
    Then the Computations Editor should have 3 separate sections:
      | Details                 |
      | Computation Properties  |
      | Time Series Parameters  |

  Scenario: Configuring the Details in the Computations editor
    When I configure the information for the following fields:
      | Comp Name |
      | Algorithm |
      | Process   |
      | Group     |
      | Effective Start |
      | Effective End   |
      | Comments        |
    Then the information configured in the Details section should persist

  Scenario: Populating the Computation Properties and Time Series Parameters section
    When I select an algorithm in the Algorithm field
    Then the Computation Properties should be populated based on the selected algorithm
    And the Time Series Parameters section should be populated based on the selected algorithm


  Scenario: Configuring and successfully saving the updates in the Computations editor
    When I configure the Details section
    And I configure the Computation Properties section
    And I configure the Time Series Parameters section
    And I click the Save button
    And I receive a confirmation message to save the computation
    And I click OK in the message
    Then I should receive a message that the computation is saved successfully
    And I click on the OK button in the message
    And the new computation should be added to the Computations page
