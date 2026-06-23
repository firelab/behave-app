@core
Feature: Crown Results - Fire Type

  @core
  Scenario: Fire Type is displayed in results when inputs are set
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group                    | value                               |
      | Fire Behavior | Fire Behavior            | Rate of Spread                      |
      | Fire Behavior | Fire Behavior            | Flame Length                        |
      | Fire Behavior | Fire Behavior            | Fireline Intensity                  |
      | Fire Type     | Active Crown Fire        | Active Ratio                        |
      | Fire Type     | Active Crown Fire        | Critical Crown Rate of Spread       |
      | Fire Type     | Transition to Crown Fire | Transition Ratio                    |
      | Fire Type     | Transition to Crown Fire | Critical Surface Flame Length       |
      | Fire Type     | Transition to Crown Fire | Critical Surface Rate of Spread     |
      | Fire Type     | Transition to Crown Fire | Critical Surface Fireline Intensity |
      | Size          | Crown - Fire Size        | Fire Area                           |
      | Size          | Crown - Fire Size        | Fire Perimeter                      |
      | Size          | Crown - Fire Size        | Spread Distance                     |
    When these input paths are selected

    Then "the following outputs are displayed in the results page"
      | output    |
      | Fire Type |