@core
Feature: Surface Input - Wind and Slope -> 20-Foot Wind Speed

  @core
  Scenario: 20-Foot Wind Speed is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value   |
      | Fire Behavior | Direction Mode | Heading |
    When these input paths are entered
      | submodule      | group             | value   |
      | Wind and Slope | Wind Measured at: | 20-Foot |
    Then the following input paths are displayed:
      | submodule      | group              |
      | Wind and Slope | 20-Foot Wind Speed |