@core
Feature: Surface Input - Fuel Moisture -> Dead, Live Herb, and Live Woody Categories -> Live Woody Fuel Moisture

  @core
  Scenario Outline: Live Woody Fuel Moisture is displayed with these Fuel Models
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group        | value        |
      | Fire Behavior | Surface Fire | Flame Length |

    And these output paths are NOT selected
      | submodule | group                     | value                    |
      | Spot      | Maximum Spotting Distance | Wind-Driven Surface Fire |

    And this input path is entered <submodule> : <group> : <subgroup> : <value>

    And these input paths are entered
      | submodule     | group               | subgroup                                   |
      | Fuel Moisture | Moisture Input Mode | Dead, Live Herb, and Live Woody Categories |

    Then the following input paths are displayed:
      | submodule     | group                                      | value                    |
      | Fuel Moisture | Dead, Live Herb, and Live Woody Categories | Live Woody Fuel Moisture |
      
    Examples: This scenario is repeated for each of these rows
      | submodule  | group    | subgroup   | value                                         |
      | Fuel Model | Standard | Fuel Model | FB10/10 - Timber litter & understory (Static) |
 
  @extended
  Scenario Outline: Live Woody Fuel Moisture is displayed with these Fuel Models (extended)
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group        | value        |
      | Fire Behavior | Surface Fire | Flame Length |

    And these output paths are NOT selected
      | submodule | group                     | value                    |
      | Spot      | Maximum Spotting Distance | Wind-Driven Surface Fire |

    And this input path is entered <submodule> : <group> : <subgroup> : <value>

    And these input paths are entered
      | submodule     | group               | subgroup                                   |
      | Fuel Moisture | Moisture Input Mode | Dead, Live Herb, and Live Woody Categories |

    Then the following input paths are displayed:
      | submodule     | group                                      | value                    |
      | Fuel Moisture | Dead, Live Herb, and Live Woody Categories | Live Woody Fuel Moisture |
      
    Examples: This scenario is repeated for each of these rows
      | submodule  | group    | subgroup   | value                                                               |
      | Fuel Model | Standard | Fuel Model | FB10/10 - Timber litter & understory (Static)                       |
      | Fuel Model | Standard | Fuel Model | GS1/121 - Low load, dry climate grass-shrub (Dynamic)               |
      | Fuel Model | Standard | Fuel Model | GS2/122 - Moderate load, dry climate grass-shrub (Dynamic)          |
      | Fuel Model | Standard | Fuel Model | GS3/123 - Moderate load, humid climate grass-shrub (Dynamic)        |
      | Fuel Model | Standard | Fuel Model | GS4/124 - High load, humid climate grass-shrub (Dynamic)            |
      | Fuel Model | Standard | Fuel Model | SH1/141 - Low load, dry climate shrub (Dynamic)                     |
      | Fuel Model | Standard | Fuel Model | SH2/142 - Moderate load, dry climate shrub (Static)                 |
      | Fuel Model | Standard | Fuel Model | SH3/143 - Moderate load, humid climate shrub (Static)               |
      | Fuel Model | Standard | Fuel Model | SH4/144 - Low load, humid climate timber-shrub (Static)             |
      | Fuel Model | Standard | Fuel Model | SH5/145 - High load, dry climate shrub (Static)                     |
      | Fuel Model | Standard | Fuel Model | SH6/146 - Low load, humid climate shrub (Static)                    |
      | Fuel Model | Standard | Fuel Model | SH7/147 - Very high load, dry climate shrub (Static)                |
      | Fuel Model | Standard | Fuel Model | SH8/148 - High load, humid climate shrub (Static)                   |
      | Fuel Model | Standard | Fuel Model | SH9/149 - Very high load, humid climate shrub (Dynamic)             |
      | Fuel Model | Standard | Fuel Model | TU1/161 - Light load, dry climate timber-grass-shrub (Dynamic)      |
      | Fuel Model | Standard | Fuel Model | TU2/162 - Moderate load, humid climate timber-shrub (Static)        |
      | Fuel Model | Standard | Fuel Model | TU3/163 - Moderate load, humid climate timber-grass-shrub (Dynamic) |
      | Fuel Model | Standard | Fuel Model | TU4/164 - Dwarf conifer understory (Static)                         |
      | Fuel Model | Standard | Fuel Model | TU5/165 - Very high load, dry climate timber-shrub (Static)         |
      | Fuel Model | Standard | Fuel Model | FB4/4 - Chaparral (Static)                                          |
      | Fuel Model | Standard | Fuel Model | FB5/5 - Brush (Static)                                              |
      | Fuel Model | Standard | Fuel Model | FB7/7 - Southern rough (Static)                                     |
