@core
Feature: Crown & Surface Input - Foliar Moisture

  @core
  Scenario Outline: Foliar Moisture is displayed
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule       |
      | Foliar Moisture |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group                    | value                               |
      | Fire Behavior | Fire Behavior            | Rate of Spread                      |
      | Fire Behavior | Fire Behavior            | Flame Length                        |
      | Fire Behavior | Fire Behavior            | Fireline Intensity                  |
      | Size          | Crown - Fire Size        | Fire Area                           |
      | Size          | Crown - Fire Size        | Fire Perimeter                      |
      | Size          | Crown - Fire Size        | Length-to-Width Ratio               |
      | Size          | Crown - Fire Size        | Spread Distance                     |
      | Fire Type     | Transition to Crown Fire | Transition Ratio                    |
      | Fire Type     | Transition to Crown Fire | Critical Surface Flame Length       |
      | Fire Type     | Transition to Crown Fire | Critical Surface Fireline Intensity |
      | Fire Type     | Active Crown Fire        | Active Ratio                        |
      | Fire Type     | Active Crown Fire        | Critical Crown Rate of Spread       |