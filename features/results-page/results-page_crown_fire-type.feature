@core
Feature: Crown Results - Fire Type

  @core
  Scenario Outline: Fire Type is displayed in results when inputs are set
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    When these input paths are selected
      | submodule         | group                       | subgroup                            | value                                |
      | Fuel Model        | Standard                    | Fuel Model                          | FB1/1 - Short grass (Static)         |
      | Fuel Moisture     | Moisture Input Mode         |                                     | Individual Size Class                |
      | Fuel Moisture     | By Size Class               | 1-h Fuel Moisture                   | 1                                    |
      | Fuel Moisture     | By Size Class               | 10-h Fuel Moisture                  | 1                                    |
      | Fuel Moisture     | By Size Class               | 100-h Fuel Moisture                 | 1                                    |
      | Fuel Moisture     | By Size Class               | Live Woody Fuel Moisture            | 60                                   |
      | Wind and Slope    | Wind Measured at:           |                                     | 20-Foot                              |
      | Wind and Slope    | 20-Foot Wind Speed          |                                     | 1                                    |
      | Wind and Slope    | Wind Adjustment Factor      |                                     | User Input                           |
      | Wind and Slope    | Wind Adjustment Factor      | Wind Adjustment Factor - User Input | 1                                    |
      | Wind and Slope    | Wind and slope are          |                                     | Aligned (Wind is ≤30° from upslope). |
      | Wind and Slope    | Slope                       |                                     | 0                                    |
      | Crown Fire Method | Calculate Crown Fire Using: |                                     | Finney                               |
      | Foliar Moisture   | Foliar Moisture             |                                     | 30                                   |
      | Canopy Fuel       | Canopy Height               |                                     | 10                                   |
      | Canopy Fuel       | Canopy Base Height          |                                     | 10                                   |
      | Canopy Fuel       | Canopy Bulk Density         |                                     | 0.5                                  |
    Then "the following outputs are displayed in the results page"
      | output    |
      | Fire Type |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group         | value          |
      | Fire Behavior | Fire Behavior | Rate of Spread |

  @extended
  Scenario Outline: Fire Type is displayed in results when inputs are set (Extended)
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    When these input paths are selected
      | submodule         | group                       | subgroup                            | value                                |
      | Fuel Model        | Standard                    | Fuel Model                          | FB1/1 - Short grass (Static)         |
      | Fuel Moisture     | Moisture Input Mode         |                                     | Individual Size Class                |
      | Fuel Moisture     | By Size Class               | 1-h Fuel Moisture                   | 1                                    |
      | Fuel Moisture     | By Size Class               | 10-h Fuel Moisture                  | 1                                    |
      | Fuel Moisture     | By Size Class               | 100-h Fuel Moisture                 | 1                                    |
      | Fuel Moisture     | By Size Class               | Live Woody Fuel Moisture            | 60                                   |
      | Wind and Slope    | Wind Measured at:           |                                     | 20-Foot                              |
      | Wind and Slope    | 20-Foot Wind Speed          |                                     | 1                                    |
      | Wind and Slope    | Wind Adjustment Factor      |                                     | User Input                           |
      | Wind and Slope    | Wind Adjustment Factor      | Wind Adjustment Factor - User Input | 1                                    |
      | Wind and Slope    | Wind and slope are          |                                     | Aligned (Wind is ≤30° from upslope). |
      | Wind and Slope    | Slope                       |                                     | 0                                    |
      | Crown Fire Method | Calculate Crown Fire Using: |                                     | Finney                               |
      | Foliar Moisture   | Foliar Moisture             |                                     | 30                                   |
      | Canopy Fuel       | Canopy Height               |                                     | 10                                   |
      | Canopy Fuel       | Canopy Base Height          |                                     | 10                                   |
      | Canopy Fuel       | Canopy Bulk Density         |                                     | 0.5                                  |
    Then "the following outputs are displayed in the results page"
      | output    |
      | Fire Type |

    Examples: This scenario is repeated for each of these rows
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