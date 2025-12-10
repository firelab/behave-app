@core
Feature: Surface Input - Spread Directions

  @core
  Scenario: Spread Directions is displayed when Direction of Interest is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value                 |
      | Fire Behavior | Direction Mode | Direction of Interest |
    Then the following input paths are displayed:
      | submodule         |
      | Spread Directions |