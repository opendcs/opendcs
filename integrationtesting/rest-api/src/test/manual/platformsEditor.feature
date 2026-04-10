Feature: Platforms Editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Platforms editor
  So that I can create or edit platform data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Platforms page
    And I load the Platforms editor

  Scenario: Display of the Platforms editor
    Then the Platforms Editor should have 4 separate sections:
      | Details                     |
      | Platform Sensor Information |
      | Properties                  |
      | Transport Media             |

  Scenario: Configuring the Details in the Platforms editor
    When I configure the information for the following fields:
      | Site         |
      | Designator   |
      | Config       |
      | Owner Agency |
      | Description  |
    And I toggle the Production switch
    Then the information configured in the Details section should persist

  Scenario: Configuring the Properties table
    When I click on the Plus button in the Properties table
    And a new row is added to the Properties table
    And I configure the content for the Property Name and Value
    Then the entered information for the Property Name and Value should persist

  Scenario: Displaying rows in the Platform Sensor Information
    When I select a config with sensors in the Config field
    Then the Platform Sensor Information section should be populated

  Scenario: Accessing the Configs editor from the Platforms editor
    When I click the edit button in the Config field
    Then a new webpage is loaded
    And the Config editor is loaded

  Scenario: Configuring the Transport Media section
    When I click the Plus button in the Transport Media table
    And the Transport Medium editor is loaded
    And I enter content for the General Details section
    And I configure the information for parameters based on the selected Medium Type
    And I click the OK button in the Transport Medium editor
    Then the configured information for the transport medium is displayed in the Transport Media section

  Scenario: Configuring and successfully saving the updates in the Platforms editor
    When I configure the Details section
    And I configure the Properties section
    And I configure the Transport Media section
    And I click the Save button
    And I receive a confirmation message to save the platform
    And I click OK in the message
    Then I should receive a message that the platform is saved successfully
    And I click on the OK button in the message
    And the new platform should be added to the Platforms page
