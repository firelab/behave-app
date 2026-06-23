@core
Feature: Surface Results - DOI Spread Distance

  @core
  Scenario: DOI Spread Distance is displayed in results when inputs are set
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group               | value                 |
      | Fire Behavior | Direction Mode      | Direction of Interest |
      | Size          | Surface - Fire Size | Spread Distance       |
    When these input paths are selected
      | submodule      | group               | subgroup          | value                                |
      | Fuel Model     | Standard            | Fuel Model        | FB1/1 - Short grass (Static)         |
      | Fuel Moisture  | Moisture Input Mode |                   | Individual Size Class                |
      | Fuel Moisture  | By Size Class       | 1-h Fuel Moisture | 1                                    |
      | Wind and Slope | Wind Measured at:   |                   | Midflame (Eye Level)                 |
      | Wind and Slope | Wind Speed          |                   | 1                                    |
      | Wind and Slope | Wind and slope are  |                   | Aligned (Wind is ≤30° from upslope). |
      | Wind and Slope | Slope               |                   | 0                                    |
    Then "the following outputs are displayed in the results page"
      | output              |
      | DOI Spread Distance |