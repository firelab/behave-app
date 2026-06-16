@core
Feature: Surface & Crown Input - Spot -> Downwind Canopy Fuel

  @core
  Scenario: Downwind Canopy Fuel is displayed when Active Crown Fire is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these output paths are selected
      | submodule | group                     | value             |
      | Spot      | Maximum Spotting Distance | Active Crown Fire |
    When these output paths are NOT selected
      | submodule     | group                    | value                               |
      | Fire Behavior | Fire Behavior            | Rate of Spread                      |
      | Fire Behavior | Fire Behavior            | Flame Length                        |
      | Fire Behavior | Fire Behavior            | Fireline Intensity                  |
      | Fire Type     | Active Crown Fire        | Active Ratio                        |
      | Fire Type     | Active Crown Fire        | Critical Crown Rate of Spread       |
      | Fire Type     | Transition to Crown Fire | Transition Ratio                    |
      | Fire Type     | Transition to Crown Fire | Critical Surface Flame Length       |
      | Fire Type     | Transition to Crown Fire | Critical Surface Fireline Intensity |
      | Size          | Crown - Fire Size        | Fire Area                           |
      | Size          | Crown - Fire Size        | Fire Perimeter                      |
      | Size          | Crown - Fire Size        | Spread Distance                     |
    Then the following input paths are displayed:
      | submodule | group                |
      | Spot      | Downwind Canopy Fuel |