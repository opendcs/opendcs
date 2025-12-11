Feature: Logging into the OpenDCS Web Client

  As a user with valid certificates
  I want to login successfully
  So that I could access the OpenDCS Web Client

  Scenario: Successfully loading the OpenDCS Web Client
    Given I have valid certificates
    When I access the URL for the OpenDCS Web Client
    And I am prompted for which certificate to use
    Then I should be transferred to a Login page

  Scenario: Successfully logging into the OpenDCS Web Client
    Given I successfully access the OpenDCS Web Client
    And I have an existing account
    When I clicks the "Login" button
    And I am prompted to enter my PIN
    And I enter my PIN
    Then I should successfully log into the OpenDCS Web Client

