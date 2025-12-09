Feature: Surface Input - Wind and Slope -> Wind Adjustment Factor -> Wind Adjustment Factor - User Input

  Scenario: Wind Adjustment Factor - User Input is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value   |
      | Fire Behavior | Direction Mode | Heading |
    When these input paths are entered
      | submodule      | group                  | value      |
      | Wind and Slope | Wind Measured at:      | 20-Foot    |
      | Wind and Slope | Wind Adjustment Factor | User Input |
    Then the following input paths are displayed:
      | submodule      | group                  | value                               |
      | Wind and Slope | Wind Adjustment Factor | Wind Adjustment Factor - User Input |