Feature: Configs Editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Configs editor
  So that I can create or edit config data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Configs page
    And I load the Configs editor

  Scenario: Display of the Configs editor
    Then there should be the following fields to configure:
      | Name          |
      | Num Platforms |
      | Description   |
    And the Configs Editor should have 2 separate sections:
      | Sensors          |
      | Decoding Scripts |

  Scenario: Configuring the Sensors section
    When I click on the Plus button in the Sensors table
    And the Config Sensor editor is loaded
    And I enter information in the Config Sensor
    And I click OK in the Config Sensor editor
    Then the new config sensor should be added as a new row in the Sensors section

  Scenario: Configuring the Decoding Scripts section
    When I click the Plus button in the Decoding Scripts table
    And the Decoding Script editor is loaded
    And I enter information in the Decoding Script editor
    And I click the OK button in the Decoding Script editor
    Then the configured information for the decoding script is displayed in the Decoding Script section

  Scenario: Configuring and successfully saving the updates in the Configs editor
    When I configure the information for the following fields
      | Name          |
      | Num Platforms |
      | Description   |
    And I configure the Sensors section
    And I configure the Decoding Script section
    And I click the Save button
    And I receive a confirmation message to save the config
    And I click OK in the message
    Then I should receive a message that the config is saved successfully
    And I click on the OK button in the message
    And the new config should be added to the Configs page
