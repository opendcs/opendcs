Feature: Netlists editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Netlists editor
  So that I can create or edit netlist data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Netlists page
    And I load the Netlists editor

  Scenario: Display of the Netlists editor
    Then there should be a Name field
    And there should be a Transport Medium Type dropdown
    And there should be a Site Name Type dropdown
    And there should be a table with the following columns:
      | Transport Medium - ID |
      | Platform ID           |
      | Platform Name         |
      | Agency                |
      | Transport Medium - ID |
      | Config                |
      | Description           |

  Scenario: Configuring and successfully saving the updates in the Netlists editor
    When I configure the Name field
    And I select an option in the Transport Medium Type dropdown
    And I select an option in the Site Name Type dropdown
    And I select a row in the table
    And I click the Save button
    And I receive a confirmation message to save the netlist
    And I click OK in the message
    Then I should receive a message that the netlist is saved successfully
    And I click on the OK button in the message
    And the new netlist should be added to the Netlistss page
