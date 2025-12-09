Feature: Surface Input - Wind and Slope -> Wind Speed

  Scenario: Wind Speed is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value   |
      | Fire Behavior | Direction Mode | Heading |
    When these input paths are entered
      | submodule      | group             | value                |
      | Wind and Slope | Wind Measured at: | Midflame (Eye Level) |
    Then the following input paths are displayed:
      | submodule      | group      |
      | Wind and Slope | Wind Speed |