@core
Feature: Crown & Surface Input - Spot -> Fire Behavior

  @core
  Scenario: Fire Behavior is displayed when Active Crown Fire is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these output paths are selected
      | submodule | group                     | value             |
      | Spot      | Maximum Spotting Distance | Active Crown Fire |
    When these output paths are NOT selected
      | submodule     | group         | value              |
      | Fire Behavior | Fire Behavior | Rate of Spread     |
      | Fire Behavior | Fire Behavior | Fireline Intensity |
      | Fire Behavior | Fire Behavior | Flame Length       |
    Then the following input paths are displayed:
      | submodule | group         |
      | Spot      | Fire Behavior |