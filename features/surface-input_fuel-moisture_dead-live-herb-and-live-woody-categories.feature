@core
Feature: Surface Input - Fuel Moisture -> Dead, Live Herb, and Live Woody Categories

  @core
  Scenario: Dead, Live Herb, and Live Woody Categories is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group        | value        |
      | Fire Behavior | Surface Fire | Flame Length |
    When these input paths are entered
      | submodule     | group               | value                                      |
      | Fuel Moisture | Moisture Input Mode | Dead, Live Herb, and Live Woody Categories |
    Then the following input paths are displayed:
      | submodule     | group                                      |
      | Fuel Moisture | Dead, Live Herb, and Live Woody Categories |