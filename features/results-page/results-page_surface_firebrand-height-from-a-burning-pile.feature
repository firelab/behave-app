@core
Feature: Surface Results - Firebrand Height from a Burning Pile

  @core
  Scenario: Firebrand Height from a Burning Pile is displayed in results when inputs are set
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule | group                     | value        |
      | Spot      | Maximum Spotting Distance | Burning Pile |
    When these input paths are selected
      | submodule      | group               | subgroup          | value                        |
      | Fuel Model     | Standard            | Fuel Model        | FB1/1 - Short grass (Static) |
      | Fuel Moisture  | Moisture Input Mode |                   | Individual Size Class        |
      | Fuel Moisture  | By Size Class       | 1-h Fuel Moisture | 1                            |
      | Wind and Slope | Wind Measured at:   |                   | Midflame (Eye Level)         |
      | Wind and Slope | Wind Speed          |                   | 1                            |
    Then "the following outputs are displayed in the results page"
      | output                               |
      | Firebrand Height from a Burning Pile |