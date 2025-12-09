Feature: Surface Input - Spot -> Burning Pile

  Scenario: Burning Pile is displayed when Burning Pile is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule | group                     | value        |
      | Spot      | Maximum Spotting Distance | Burning Pile |
    Then the following input paths are displayed:
      | submodule | group        |
      | Spot      | Burning Pile |
