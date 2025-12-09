Feature: Surface Input - Spread Directions -> Direction of Interest

  Scenario: Direction of Interest is displayed when Direction of Interest is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value                 |
      | Fire Behavior | Direction Mode | Direction of Interest |
    Then the following input paths are displayed:
      | submodule         | group                 |
      | Spread Directions | Direction of Interest |