@Kiwi.Plan(DbEdit)
@Kiwi.Priority(Medium)
Feature: Platform List Tab

    Background: Basic Setup
        Given A database with many platforms available.
    
    Scenario: Sorting by column

        Given The list is sorted by any column
        When The user double clicks a row
        Then The selected platform is returned

        When The user selects a row and clicked open
        Then The selected platform is opened
  

    Scenario Outline: Filter by <filter>

        Given The user filters the platform list by <filter> selection
        When The user double clicks a row
        Then The correct platform is opened
        
        Given The user filters the platform list by <filter>
        When The user selects a row and clicks open.
        Then The correct platform is opened

    Examples:
        | filter     |
        |platform    |
        |Agency      |
        |Transport-ID|
        |Config      |