@Kiwi.Plan(DbEdit)
@Kiwi.Priority(Medium)
Feature: Property selection in a Site
  
  Background: Basic Setup
        Given A database with sites available the have more than 1 extra property. Create additional properties on a site if necessary.
          And The user is on the Sites tab

    Scenario Outline: User sorts properties by <column>

        Given The user opens a Site.
          And The user sorts the site properties by <column>
         When The user select a row and edits the Property
         Then A dialog with the correct property name and valume are shown.

         When The user hovers the mouse over a property
         Then The correct tooltip is shown. NOTE: you may have to determine this by context.

    Examples:
        | column |
        | name   |
        | value  |