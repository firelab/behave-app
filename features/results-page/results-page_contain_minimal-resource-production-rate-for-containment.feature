@core
Feature: Contain Results - Minimal Resource Production Rate for Containment

  @core
  Scenario: Minimal Resource Production Rate for Containment is displayed in results when inputs are set
    Given I have started a new Surface & Contain Worksheet in Guided Mode
    When these input paths are selected
      | submodule      | group                                        | subgroup              | value                                  |
      | Suppression    | Contain Mode                                 |                       | Calculate Minimum Production Rate Only |
      | Suppression    | Tactic                                       |                       | Head                                   |
      | Suppression    | Line Construction Offset                     |                       | 0                                      |
      | Suppression    | Fire Area at Report                          |                       | 1                                      |
      | Suppression    | Estimated Resource Arrival Time and Duration | Resource Arrival Time | 1                                      |
      | Suppression    | Estimated Resource Arrival Time and Duration | Resource Duration     | 8                                      |
      | Fuel Model     | Standard                                     | Fuel Model            | FB1/1 - Short grass (Static)           |
      | Fuel Moisture  | Moisture Input Mode                          |                       | Individual Size Class                  |
      | Fuel Moisture  | By Size Class                                | 1-h Fuel Moisture     | 1                                      |
      | Wind and Slope | Wind Measured at:                            |                       | Midflame (Eye Level)                   |
      | Wind and Slope | Wind Speed                                   |                       | 1                                      |
      | Wind and Slope | Wind and slope are                           |                       | Aligned (Wind is ≤30° from upslope).   |
      | Wind and Slope | Slope                                        |                       | 0                                      |
    Then "the following outputs are displayed in the results page"
      | output                                           |
      | Minimal Resource Production Rate for Containment |