Feature: Surface Input - Wind and Slope -> Wind and slope are -> Wind Direction

  Scenario: Wind Direction is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value   |
      | Fire Behavior | Direction Mode | Heading |
    When these input paths are entered
      | submodule      | group              | value                                    |
      | Wind and Slope | Wind and slope are | Not Aligned (Wind is >30° from upslope). |
    Then the following input paths are displayed:
      | submodule      | group              | value          |
      | Wind and Slope | Wind and slope are | Wind Direction |
