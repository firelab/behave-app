@core
Feature: Surface Input - Spot -> Surface Fire Flame Length

  @core
  Scenario: Surface Fire Flame Length is displayed when Wind-Driven Surface Fire (Grass Only) is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule | group                     | value                                 |
      | Spot      | Maximum Spotting Distance | Burning Pile                          |
      | Spot      | Maximum Spotting Distance | Wind-Driven Surface Fire (Grass Only) |
    When these output paths are NOT selected
      | submodule     | group          | value                      |
      | Fire Behavior | Direction Mode | Heading                    |
      | Fire Behavior | Direction Mode | Heading, Flanking, Backing |
      | Fire Behavior | Direction Mode | Direction of Interest      |
    Then the following input paths are displayed:
      | submodule | group                     |
      | Spot      | Surface Fire Flame Length |