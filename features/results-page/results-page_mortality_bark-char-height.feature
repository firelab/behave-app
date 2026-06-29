@core
Feature: Mortality Results - Bark Char Height

  @core
  Scenario Outline: Bark Char Height is displayed in results when inputs are set
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value          |
      | Fire Behavior | Direction Mode | Heading        |
      | Fire Behavior | Surface Fire   | Rate of Spread |
    When these input paths are selected
      | submodule      | group               | subgroup          | value                                |
      | Fuel Model     | Standard            | Fuel Model        | FB1/1 - Short grass (Static)         |
      | Fuel Moisture  | Moisture Input Mode |                   | Individual Size Class                |
      | Fuel Moisture  | By Size Class       | 1-h Fuel Moisture | 1                                    |
      | Wind and Slope | Wind Measured at:   |                   | Midflame (Eye Level)                 |
      | Wind and Slope | Wind Speed          |                   | 1                                    |
      | Wind and Slope | Wind and slope are  |                   | Aligned (Wind is ≤30° from upslope). |
      | Wind and Slope | Slope               |                   | 0                                    |
    When this input path is entered <submodule> : <group> : <value>
    When these input paths are selected
      | submodule            | group                           | value |
      | Tree Characteristics | DBH (Diameter at Breast Height) | 10    |
    Then "the following outputs are displayed in the results page"
      | output           |
      | Bark Char Height |

    Examples: This scenario is repeated for each of these rows
      | submodule            | group                  | value                          |
      | Tree Characteristics | Mortality Tree Species | Acer rubrum / ACRU (Red maple) |

  @extended
  Scenario Outline: Bark Char Height is displayed in results when inputs are set (Extended)
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value          |
      | Fire Behavior | Direction Mode | Heading        |
      | Fire Behavior | Surface Fire   | Rate of Spread |
    When these input paths are selected
      | submodule      | group               | subgroup          | value                                |
      | Fuel Model     | Standard            | Fuel Model        | FB1/1 - Short grass (Static)         |
      | Fuel Moisture  | Moisture Input Mode |                   | Individual Size Class                |
      | Fuel Moisture  | By Size Class       | 1-h Fuel Moisture | 1                                    |
      | Wind and Slope | Wind Measured at:   |                   | Midflame (Eye Level)                 |
      | Wind and Slope | Wind Speed          |                   | 1                                    |
      | Wind and Slope | Wind and slope are  |                   | Aligned (Wind is ≤30° from upslope). |
      | Wind and Slope | Slope               |                   | 0                                    |
    When this input path is entered <submodule> : <group> : <value>
    When these input paths are selected
      | submodule            | group                           | value |
      | Tree Characteristics | DBH (Diameter at Breast Height) | 10    |
    Then "the following outputs are displayed in the results page"
      | output           |
      | Bark Char Height |

    Examples: This scenario is repeated for each of these rows
      | submodule            | group                  | value                                          |
      | Tree Characteristics | Mortality Tree Species | Acer rubrum / ACRU (Red maple)                 |
      | Tree Characteristics | Mortality Tree Species | Cornus florida / COFL2 (Flowering dogwood)     |
      | Tree Characteristics | Mortality Tree Species | Nyssa sylvatica / NYBI (Blackgum)              |
      | Tree Characteristics | Mortality Tree Species | Nyssa sylvatica / NYSY (Blackgum)              |
      | Tree Characteristics | Mortality Tree Species | Oxydendrum arboreum / OXAR (Sourwood)          |
      | Tree Characteristics | Mortality Tree Species | Quercus alba / QUAL (White oak)                |
      | Tree Characteristics | Mortality Tree Species | Quercus bicolor / QUBI (Swamp white oak)       |
      | Tree Characteristics | Mortality Tree Species | Quercus coccinea / QUCO2 (Scarlet oak)         |
      | Tree Characteristics | Mortality Tree Species | Quercus garryana / QUGA4 (Oregon white oak)    |
      | Tree Characteristics | Mortality Tree Species | Quercus kelloggii / QUKE (Califonia black oak) |
      | Tree Characteristics | Mortality Tree Species | Quercus marilandica / QUMA3 (Blackjack oak)    |
      | Tree Characteristics | Mortality Tree Species | Quercus velutina / QUVE (Black oak)            |
      | Tree Characteristics | Mortality Tree Species | Sassafras albidum / SAAL5 (Sassafras)          |