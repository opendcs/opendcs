@Kiwi.Plan(DbEdit)
@Kiwi.Priority(Medium)
Feature: Site List Sorting on the Sites Tab

    Background: Basic Setup

        Given A database with many sites

        Scenario Outline: Sorted by <column> Opening by <open method>

            Given The list is sorted by <column>

            When The user opens a site with <open method>

            Then The correct site is opened

        Examples:
            |open method                   | column      |
            |selects a row and clicks Open | local       |
            |Double Click Row              | local       |
            |selects a row and clicks Open | description |
            |Double Click Row              | description |
