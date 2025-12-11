Feature: Sites Editor Functionality

  As a user of the OpenDCS Web Client
  I want to work in the Sites editor
  So that I can create or edit site data

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the Sites page
    And I load the Sites editor

  Scenario: Display of the Sites editor
    Then the Sites Editor should have 3 separate sections:
      | Details    |
      | Site Names |
      | Site Names |

  Scenario: Configuring the Details in the Sites editor
    When I configure the information for the following fields:
      | Latitude      |
      | Longitude     |
      | Elevation     |
      | Elev Units    |
      | Nearest City  |
      | Time Zone     |
      | State         |
      | Country       |
      | Region        |
      | Public Name   |
      | Description   |
    Then the information configured in the Details section should persist

  Scenario: Configuring the Site Names table
    When I click on the Plus button in the Site Names table
    And a new row is added to the Site Names table
    And I configure the content for the Type and Identifier
    Then the entered information for the Type and Identifier should persist

  Scenario: Configuring the Properties table
    When I click on the Plus button in the Properties table
    And a new row is added to the Properties table
    And I configure the content for the Property Name and Value
    Then the entered information for the Property Name and Value should persist

  Scenario: Configuring and successfully saving the updates in the Sites editor
    When I configure the Details section
    And I configure the Site Names section
    And I configure the Properties section
    And I click the Save button
    And I receive a confirmation message to save the site
    And I click OK in the message
    Then I should receive a message that the site is saved successfully
    And I click on the OK button in the message
    And the new site should be added to the Sites page
