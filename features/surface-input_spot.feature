Feature: Surface Input - Spot

  Scenario Outline: Spot is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule |
      | Spot      |
    Examples: This scenario is repeated for each of these rows
      | submodule | group                     | value                    |
      | Spot      | Maximum Spotting Distance | Burning Pile             |
      | Spot      | Maximum Spotting Distance | Wind-Driven Surface Fire |
