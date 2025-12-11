Feature: General Display of the OpenDCS Web Client

  As a user of the OpenDCS Web Client
  I want to verify the general display of the OpenDCS Web Client
  So that I can verify the default display of the OpenDCS Web Client

  Background:
    Given I am successfully logged into the OpenDCS Web Client
    And I am on the default page of the OpenDCS Web Client

  Scenario: Display of the groups available in the OpenDCS Web Client
    Then the OpenDCS Web Client should have 3 groups:
     | Decodes Database Editor |
     | Computations            |
     | Reflist Editor          |

  Scenario: Display of the Decodes Database Editor group's Platform page by default
    Then the Decodes Database Editor group should be expanded
    And the Platforms page is displayed

   Scenario: Display of the Menu
    Then the Menu should be displayed in the upper left corner.

  Scenario: Clicking the Menu
    When I click on the "Menu" button
    Then The whole menu should collapse

  Scenario: Hover over items in collapsed menu
    Given the menu is collapsed
    When I hover over the menu
    Then the menu should list the 3 groups of:
      | Decodes Database Editor |
      | Computations            |
      | Reflist Editor          |

  Scenario: Determine the number of items under the Decodes Database Editor Items
    When I select the Decodes Database Editor option in the menu
    Then There should be 8 options:
      | Platforms       |
      | Sites           |
      | Configs         |
      | Presentation    |
      | Routing         |
      | Sources         |
      | Netlists        |
      | Schedule Entry  |

  Scenario: Determine the number of items under the Computations Items
    When I select the Computations option in the menu
    Then There should be 3 options:
      | Algorithms   |
      | Computations |
      | Processes    |

  Scenario: Determine the number of items under the Reflist Editor Items
    When I select the Reflist option in the menu
    Then There should be 4 options:
      | Enumerations      |
      | Engineering Units |
      | EU Conversions    |
      | Seasons |

  Scenario Outline: Using search filters for each page in the OpenDCS Web Client
    When I am on the <Page> page
    And I enter "<Text>" in the Search field
    Then the content filters to rows that contain "<Text>"

    Examples:
      | Page              | Text  |
      | Platforms         | nws   |
      | Sites             | wms   |
      | Configs           | wms   |
      | Presentation      | wms   |
      | Routing           | nws   |
      | Sources           | nwp   |
      | Netlists          | nwp   |
      | Schedule Entry    | nwp   |
      | Algorithms        | dec   |
      | Computations      | nws   |
      | Processes         | nwp   |
      | Engineering Units | flo   |
      | EU Conversions    | lin   |

  Scenario Outline: Default number of entries displayed in a page
    When I click on the <Page> page
    Then there should be <Number_of_Rows> rows displayed

    Examples:
      | Page              | Number_of_Rows  |
      | Platforms         | 10              |
      | Sites             | 10              |
      | Configs           | 10              |
      | Presentation      | 10              |
      | Routing           | 10              |
      | Sources           | 10              |
      | Netlists          | 10              |
      | Schedule Entry    | 10              |
      | Algorithms        | all             |
      | Computations      | all             |
      | Processes         | all             |
      | Enumerations      | all             |
      | Engineering Units | all             |
      | EU Conversions    | all             |
      | Seasons           | all             |

