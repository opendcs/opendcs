Feature: Presentation Element Editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Presentation Element editor
  So that I can create or edit presentation data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Presentation Element page
    And I load the Presentation Element editor

  Scenario: Display of the Presentation Element editor
    Then the Group Name field should be present
    And the Inherits From toggle should be present
    And there should be a table with the following columns:
      | Data Type Standard  |
      | Data Type Code      |
      | Units               |
      | Fractional Digits   |
      | Min Value           |
      | Max Value           |
      | Actions             |

  Scenario: Configuring and successfully saving the updates in the Presentation Element editor
    When I configure the the information for the Group Name field
    And I set the Inherits From toggle
    And I click the Plus button
    And a row is added to the table in the Presentation Element editor
    And I configure the information for the following fields:
      | Data Type Standard  |
      | Data Type Code      |
      | Units               |
      | Fractional Digits   |
      | Min Value           |
      | Max Value           |
      | Actions             |
    And I click the Save button
    And I receive a confirmation message to save the presentation
    And I click OK in the message
    Then I should receive a message that the presentation is saved successfully
    And I click on the OK button in the message
    And the new presentation should be added to the Presentation Element page
