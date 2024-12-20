@Kiwi.Plan(DbEdit)
@Kiwi.Priority(Medium)
Feature: Site List Sorting on the Sites Tab

    Background: Basic Setup

        Given A database with many sites

        Scenario Outline: Sorted by <column> Opening by <open method>

            Given The list is sorted by <column>

            When The user <open method>

            Then The correct site is opened

        Examples:
            | open method             | column      |
            |Select Row -> Click edit | local       |
            |Double Click Row         | local       |
            |Select Row -> Click edit | description |
            |Double Click Row         | description |
