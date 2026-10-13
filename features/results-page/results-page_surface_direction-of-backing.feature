@core
Feature: Surface Results - Direction of Backing

  @core
  Scenario: Direction of Backing is displayed in results when inputs are set
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value                      |
      | Fire Behavior | Direction Mode | Heading, Flanking, Backing |
      | Fire Behavior | Surface Fire   | Rate of Spread             |
    When these input paths are selected
      | submodule      | group               | subgroup                      | value                                    |
      | Fuel Model     | Standard            | Fuel Model                    | FB1/1 - Short grass (Static)             |
      | Fuel Moisture  | Moisture Input Mode |                               | Individual Size Class                    |
      | Fuel Moisture  | By Size Class       | 1-h Fuel Moisture             | 1                                        |
      | Wind and Slope | Wind Measured at:   |                               | Midflame (Eye Level)                     |
      | Wind and Slope | Wind Speed          |                               | 1                                        |
      | Wind and Slope | Wind and slope are  |                               | Not Aligned (Wind is >30° from upslope). |
      | Wind and Slope | Wind and slope are  | Wind Direction (from upslope) | 45                                       |
      | Wind and Slope | Slope               |                               | 0                                        |
    Then "the following outputs are displayed in the results page"
      | output               |
      | Direction of Backing |