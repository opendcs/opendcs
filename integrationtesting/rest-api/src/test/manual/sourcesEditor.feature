Feature: Sources editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Sources editor
  So that I can create or edit source data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Sources page
    And I load the Sources editor

  Scenario: Display of the Sources editor
    Then there should be a Name field
    And there should be a Type dropdown
    And a Group Members table
    And there should be a Properties table with the following columns:
      | Property Name |
      | Value         |

  Scenario: Configuring and successfully saving the updates in the Sources editor
    When I configure the Name field
    And select an option in the Type dropdown
    And I configure the information in the Properties table
    And I click the Save button
    And I receive a confirmation message to save the source
    And I click OK in the message
    Then I should receive a message that the source is saved successfully
    And I click on the OK button in the message
    And the new source should be added to the Sourcess page
