@core
Feature: Surface Input - Fuel Moisture -> By Size Class

  @core
  Scenario: By Size Class is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group        | value        |
      | Fire Behavior | Surface Fire | Flame Length |
    When these input paths are entered
      | submodule     | group               | value                 |
      | Fuel Moisture | Moisture Input Mode | Individual Size Class |
    Then the following input paths are displayed:
      | submodule     | group         |
      | Fuel Moisture | By Size Class |