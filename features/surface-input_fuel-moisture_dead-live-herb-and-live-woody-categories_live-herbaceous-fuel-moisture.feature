Feature: Surface Input - Fuel Moisture -> Dead, Live Herb, and Live Woody Categories -> Live Herbaceous Fuel Moisture

  Scenario Outline: Live Herbaceous Fuel Moisture is displayed with these Fuel Models
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group        | value        |
      | Fire Behavior | Surface Fire | Flame Length |

    When these output paths are NOT selected
      | submodule | group                     | value                    |
      | Spot      | Maximum Spotting Distance | Wind-Driven Surface Fire |

    When this input path is entered <submodule> : <group> : <subgroup> : <value>

    When these input paths are entered
      | submodule     | group               | subgroup                                   |
      | Fuel Moisture | Moisture Input Mode | Dead, Live Herb, and Live Woody Categories |

    Then the following input paths are displayed:
      | submodule     | group                                      | value                         |
      | Fuel Moisture | Dead, Live Herb, and Live Woody Categories | Live Herbaceous Fuel Moisture |

    Examples: This scenario is repeated for each of these rows
      | submodule  | group    | subgroup   | value                                                                                                             |
      | Fuel Model | Standard | Fuel Model | GR1/101 - Short, sparse, dry climate grass (Dynamic)                                                              |
      | Fuel Model | Standard | Fuel Model | GR2/102 - Low load, dry climate grass (Dynamic)                                                                   |
      | Fuel Model | Standard | Fuel Model | GR3/103 - Low load, very coarse, humid climate grass (Dynamic)                                                    |
      | Fuel Model | Standard | Fuel Model | GR4/104 - Moderate load, dry climate grass (Dynamic)                                                              |
      | Fuel Model | Standard | Fuel Model | GR5/105 - Low load, humid climate grass (Dynamic)                                                                 |
      | Fuel Model | Standard | Fuel Model | GR6/106 - Moderate load, humid climate grass (Dynamic)                                                            |
      | Fuel Model | Standard | Fuel Model | GR7/107 - High load, dry climate grass (Dynamic)                                                                  |
      | Fuel Model | Standard | Fuel Model | GR8/108 - High load, very coarse, humid climate grass (Dynamic)                                                   |
      | Fuel Model | Standard | Fuel Model | GR9/109 - Very high load, humid climate grass (Dynamic)                                                           |
      | Fuel Model | Standard | Fuel Model | V-Ha/111 - Tall Grass, > 0.5 m (Dynamic)                                                                          |
      | Fuel Model | Standard | Fuel Model | GS1/121 - Low load, dry climate grass-shrub (Dynamic)                                                             |
      | Fuel Model | Standard | Fuel Model | GS2/122 - Moderate load, dry climate grass-shrub (Dynamic)                                                        |
      | Fuel Model | Standard | Fuel Model | GS3/123 - Moderate load, humid climate grass-shrub (Dynamic)                                                      |
      | Fuel Model | Standard | Fuel Model | GS4/124 - High load, humid climate grass-shrub (Dynamic)                                                          |
      | Fuel Model | Standard | Fuel Model | SH1/141 - Low load, dry climate shrub (Dynamic)                                                                   |
      | Fuel Model | Standard | Fuel Model | SH9/149 - Very high load, humid climate shrub (Dynamic)                                                           |
      | Fuel Model | Standard | Fuel Model | SCAL17/150 - Chamise with Moderate Load Grass, 4 feet (Static)                                                    |
      | Fuel Model | Standard | Fuel Model | SCAL15/151 - Chamise with Low Load Grass, 3 feet (Static)                                                         |
      | Fuel Model | Standard | Fuel Model | SCAL16/152 - North Slope Ceanothus with Moderate Load Grass (Static)                                              |
      | Fuel Model | Standard | Fuel Model | SCAL14/153 - Manzanita/Scrub Oak with Low Load Grass (Static)                                                     |
      | Fuel Model | Standard | Fuel Model | SCAL18/154 - Coastal Sage/Buckwheat Scrub with Low Load Grass (Static)                                            |
      | Fuel Model | Standard | Fuel Model | V-MH/155 - Short Green Shrub < 1 m With Grass, Discontinuous (< 1 m) often discontinuous and with grass (Dynamic) |
      | Fuel Model | Standard | Fuel Model | V-MMb/156 - Short Shrub < 1 m, Low Dead Fraction and/or Thick Foliage (Static)                                    |
      | Fuel Model | Standard | Fuel Model | V-MAb/157 - Short Shrub < 1 m, High Dead Fraction and/or Thin Fuel (Static)                                       |
      | Fuel Model | Standard | Fuel Model | V-MMa/158 - Tall Shrub > 1 m, Low Dead Fraction and/or Thick Foliage (Static)                                     |
      | Fuel Model | Standard | Fuel Model | V-MAa/159 - Tall Shrub > 1 m, High Dead Fraction and/or Thin Fuel (Static)                                        |
      | Fuel Model | Standard | Fuel Model | TU1/161 - Light load, dry climate timber-grass-shrub (Dynamic)                                                    |
      | Fuel Model | Standard | Fuel Model | TU3/163 - Moderate load, humid climate timber-grass-shrub (Dynamic)                                               |
      | Fuel Model | Standard | Fuel Model | M-EUCd/166 - Discontinuous Litter Eucalyptus Plantation, With or Without Shrub Understory (Static)                |
      | Fuel Model | Standard | Fuel Model | M-H/167 - Deciduous or Conifer Litter, Shrub and Herb Understory                                                  |
      | Fuel Model | Standard | Fuel Model | M-F/168 - Deciduous or Conifer Litter, Shrub and Fern Understory (Dynamic)                                        |
      | Fuel Model | Standard | Fuel Model | M-CAD/169 - Deciduous Litter, Shrub Understory (Static)                                                           |
      | Fuel Model | Standard | Fuel Model | M-ESC/170 - Sclerophyll Broadleaf Litter, Shrub Understory (Static)                                               |
      | Fuel Model | Standard | Fuel Model | M-PIN/171 - Medium-Long Needle Pine Litter, Shrub Understory (Static)                                             |
      | Fuel Model | Standard | Fuel Model | M-EUC/172 - Eucalyptus Litter, Shrub Understory (Static)                                                          |
      | Fuel Model | Standard | Fuel Model | F-RAC/190 - Very Compact Litter, Short Needle Conifers (Static)                                                   |
      | Fuel Model | Standard | Fuel Model | F-FOL/191 - Compact Litter, Deciduous or Evergreen Foliage (Static)                                               |
      | Fuel Model | Standard | Fuel Model | F-EUC/193 - Pure Eucalyptus Litter, No Understory (Static)                                                        |
      | Fuel Model | Standard | Fuel Model | FB2/2 - Timber grass and understory (Static)                                                                      |
