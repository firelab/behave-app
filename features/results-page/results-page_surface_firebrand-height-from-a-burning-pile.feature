@core
Feature: Surface Results - Firebrand Height from a Burning Pile

  @core
  Scenario: Firebrand Height from a Burning Pile is displayed in results when inputs are set
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule | group                     | value        |
      | Spot      | Maximum Spotting Distance | Burning Pile |
    When these input paths are selected
      | submodule      | group                | subgroup                             | value          |
      | Wind and Slope | Wind Measured at:    |                                      | 20-Foot        |
      | Wind and Slope | 20-Foot Wind Speed   |                                      | 1              |
      | Spot           | Burning Pile         | Flame Height from a Burning Pile     | 1              |
      | Spot           | Downwind Canopy Fuel | Downwind Canopy Height               | 1              |
      | Spot           | Downwind Canopy Fuel | Downwind Canopy Cover                | Closed         |
      | Spot           | Topography           | Ridge-to-Valley Elevation Difference | 1000           |
      | Spot           | Topography           | Ridge-to-Valley Horizontal Distance  | 1              |
      | Spot           | Topography           | Spotting Source Location             | RT (Ridge Top) |
    Then "the following outputs are displayed in the results page"
      | output                               |
      | Firebrand Height from a Burning Pile |