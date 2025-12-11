Feature: Seasons Page Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Seasons page
  So that I can create, retrieve, update and delete season data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Seasons page

  Scenario: Display of the Seasons page
    Then the Seasons page should have the following 6 columns:
      | Abbreviation     |
      | Descriptive Name |
      | Start            |
      | End              |
      | TZ               |
      | Actions          |

  Scenario: Creating a new season
    When I click the Plus button
    And a new row is added to the Seasons page
    And I enter information for the new season
    And I click the Save button
    And I receive a confirmation message to save the season
    And I click Yes in the message
    Then I should receive a message that the season is saved successfully
    And I click on the OK button in the message
    And the new season should persist in the Seasons page

  Scenario: Editing the information for a season
    When I click on an existing row in the Seasons page
    And I edit the information for the season
    And I click the Save button
    And I receive a confirmation message to save the season
    And I click Yes in the message
    Then I should receive a message that the season is saved successfully

  Scenario: Deleting a row
    When I select the Delete option from a row's Actions button
    And I receive a confirmation message to delete the season
    And I click the Yes button in the confirmation message
    Then I should receive a message that the season is successfully deleted
    And I click OK in the message
    And the season should be removed from the Seasons page