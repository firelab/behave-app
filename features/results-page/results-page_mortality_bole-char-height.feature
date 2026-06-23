@core
Feature: Mortality Results - Bole Char Height

  @core
  Scenario: Bole Char Height is displayed in results when inputs are set
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value          |
      | Fire Behavior | Direction Mode | Heading        |
      | Fire Behavior | Surface Fire   | Rate of Spread |
    When these input paths are selected
      | submodule            | group                           | subgroup          | value                                |
      | Fuel Model           | Standard                        | Fuel Model        | FB1/1 - Short grass (Static)         |
      | Fuel Moisture        | Moisture Input Mode             |                   | Individual Size Class                |
      | Fuel Moisture        | By Size Class                   | 1-h Fuel Moisture | 1                                    |
      | Wind and Slope       | Wind Measured at:               |                   | Midflame (Eye Level)                 |
      | Wind and Slope       | Wind Speed                      |                   | 1                                    |
      | Wind and Slope       | Wind and slope are              |                   | Aligned (Wind is ≤30° from upslope). |
      | Wind and Slope       | Slope                           |                   | 0                                    |
      | Tree Characteristics | Mortality Tree Species          |                   | Acer rubrum / ACRU (Red maple)       |
      | Tree Characteristics | Canopy Height                   |                   | 10                                   |
      | Tree Characteristics | Crown Ratio                     |                   | 0.5                                  |
      | Tree Characteristics | DBH (Diameter at Breast Height) |                   | 10                                   |
      | Scorch               | Air Temperature                 |                   | 70                                   |
    Then "the following outputs are displayed in the results page"
      | output           |
      | Bole Char Height |