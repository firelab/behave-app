@core
Feature: Crown & Surface Input - Spot -> Torching Trees

  @core
  Scenario: Torching Trees is displayed when Torching Trees is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these output paths are selected
      | submodule | group                     | value          |
      | Spot      | Maximum Spotting Distance | Torching Trees |
    Then the following input paths are displayed:
      | submodule | group          |
      | Spot      | Torching Trees |