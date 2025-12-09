Feature: Surface Input - Fuel Moisture -> Dead, Live Herb, and Live Woody Categories -> Dead Fuel Moisture

  Scenario Outline: Dead Fuel Moisture is displayed with these Fuel Model Codes
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
      | submodule     | group                                      | value              |
      | Fuel Moisture | Dead, Live Herb, and Live Woody Categories | Dead Fuel Moisture |

    Examples: This scenario is repeated for each of these rows
      | submodule  | group    | subgroup   | value                                                                                                             |
      | Fuel Model | Standard | Fuel Model | FB1/1 - Short grass (Static)                                                                                      |
      | Fuel Model | Standard | Fuel Model | FB10/10 - Timber litter & understory (Static)                                                                     |
      | Fuel Model | Standard | Fuel Model | GR1/101 - Short, sparse, dry climate grass (Dynamic)                                                              |
      | Fuel Model | Standard | Fuel Model | GR2/102 - Low load, dry climate grass (Dynamic)                                                                   |
      | Fuel Model | Standard | Fuel Model | GR3/103 - Low load, very coarse, humid climate grass (Dynamic)                                                    |
      | Fuel Model | Standard | Fuel Model | GR4/104 - Moderate load, dry climate grass (Dynamic)                                                              |
      | Fuel Model | Standard | Fuel Model | GR5/105 - Low load, humid climate grass (Dynamic)                                                                 |
      | Fuel Model | Standard | Fuel Model | GR6/106 - Moderate load, humid climate grass (Dynamic)                                                            |
      | Fuel Model | Standard | Fuel Model | GR7/107 - High load, dry climate grass (Dynamic)                                                                  |
      | Fuel Model | Standard | Fuel Model | GR8/108 - High load, very coarse, humid climate grass (Dynamic)                                                   |
      | Fuel Model | Standard | Fuel Model | GR9/109 - Very high load, humid climate grass (Dynamic)                                                           |
      | Fuel Model | Standard | Fuel Model | FB11/11 - Light logging slash (Static)                                                                            |
      | Fuel Model | Standard | Fuel Model | V-Ha/111 - Tall Grass, > 0.5 m (Dynamic)                                                                          |
      | Fuel Model | Standard | Fuel Model | FB12/12 - Medium logging slash (Static)                                                                           |
      | Fuel Model | Standard | Fuel Model | GS1/121 - Low load, dry climate grass-shrub (Dynamic)                                                             |
      | Fuel Model | Standard | Fuel Model | GS2/122 - Moderate load, dry climate grass-shrub (Dynamic)                                                        |
      | Fuel Model | Standard | Fuel Model | GS3/123 - Moderate load, humid climate grass-shrub (Dynamic)                                                      |
      | Fuel Model | Standard | Fuel Model | GS4/124 - High load, humid climate grass-shrub (Dynamic)                                                          |
      | Fuel Model | Standard | Fuel Model | FB13/13 - Heavy logging slash (Static)                                                                            |
      | Fuel Model | Standard | Fuel Model | SH1/141 - Low load, dry climate shrub (Dynamic)                                                                   |
      | Fuel Model | Standard | Fuel Model | SH2/142 - Moderate load, dry climate shrub (Static)                                                               |
      | Fuel Model | Standard | Fuel Model | SH3/143 - Moderate load, humid climate shrub (Static)                                                             |
      | Fuel Model | Standard | Fuel Model | SH4/144 - Low load, humid climate timber-shrub (Static)                                                           |
      | Fuel Model | Standard | Fuel Model | SH5/145 - High load, dry climate shrub (Static)                                                                   |
      | Fuel Model | Standard | Fuel Model | SH6/146 - Low load, humid climate shrub (Static)                                                                  |
      | Fuel Model | Standard | Fuel Model | SH7/147 - Very high load, dry climate shrub (Static)                                                              |
      | Fuel Model | Standard | Fuel Model | SH8/148 - High load, humid climate shrub (Static)                                                                 |
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
      | Fuel Model | Standard | Fuel Model | TU2/162 - Moderate load, humid climate timber-shrub (Static)                                                      |
      | Fuel Model | Standard | Fuel Model | TU3/163 - Moderate load, humid climate timber-grass-shrub (Dynamic)                                               |
      | Fuel Model | Standard | Fuel Model | TU4/164 - Dwarf conifer understory (Static)                                                                       |
      | Fuel Model | Standard | Fuel Model | TU5/165 - Very high load, dry climate timber-shrub (Static)                                                       |
      | Fuel Model | Standard | Fuel Model | M-EUCd/166 - Discontinuous Litter Eucalyptus Plantation, With or Without Shrub Understory (Static)                |
      | Fuel Model | Standard | Fuel Model | M-H/167 - Deciduous or Conifer Litter, Shrub and Herb Understory                                                  |
      | Fuel Model | Standard | Fuel Model | M-F/168 - Deciduous or Conifer Litter, Shrub and Fern Understory (Dynamic)                                        |
      | Fuel Model | Standard | Fuel Model | M-CAD/169 - Deciduous Litter, Shrub Understory (Static)                                                           |
      | Fuel Model | Standard | Fuel Model | M-ESC/170 - Sclerophyll Broadleaf Litter, Shrub Understory (Static)                                               |
      | Fuel Model | Standard | Fuel Model | M-PIN/171 - Medium-Long Needle Pine Litter, Shrub Understory (Static)                                             |
      | Fuel Model | Standard | Fuel Model | M-EUC/172 - Eucalyptus Litter, Shrub Understory (Static)                                                          |
      | Fuel Model | Standard | Fuel Model | TL1/181 - Low load, compact conifer litter (Static)                                                               |
      | Fuel Model | Standard | Fuel Model | TL2/182 - Low load broadleaf litter (Static)                                                                      |
      | Fuel Model | Standard | Fuel Model | TL3/183 - Moderate load conifer litter (Static)                                                                   |
      | Fuel Model | Standard | Fuel Model | TL4/184 - Small downed logs (Static)                                                                              |
      | Fuel Model | Standard | Fuel Model | TL5/185 - High load conifer litter (Static)                                                                       |
      | Fuel Model | Standard | Fuel Model | TL6/186 - Moderate load broadleaf litter (Static)                                                                 |
      | Fuel Model | Standard | Fuel Model | TL7/187 - Large downed logs (Static)                                                                              |
      | Fuel Model | Standard | Fuel Model | TL8/188 - Long-needle litter (Static)                                                                             |
      | Fuel Model | Standard | Fuel Model | TL9/189 - Very high load broadleaf litter (Static)                                                                |
      | Fuel Model | Standard | Fuel Model | F-RAC/190 - Very Compact Litter, Short Needle Conifers (Static)                                                   |
      | Fuel Model | Standard | Fuel Model | F-FOL/191 - Compact Litter, Deciduous or Evergreen Foliage (Static)                                               |
      | Fuel Model | Standard | Fuel Model | F-PIN/192 - Litter from Medium-Long Needle Pine Trees (Static)                                                    |
      | Fuel Model | Standard | Fuel Model | F-EUC/193 - Pure Eucalyptus Litter, No Understory (Static)                                                        |
      | Fuel Model | Standard | Fuel Model | FB2/2 - Timber grass and understory (Static)                                                                      |
      | Fuel Model | Standard | Fuel Model | SB1/201 - Low load activity fuel (Static)                                                                         |
      | Fuel Model | Standard | Fuel Model | SB2/202 - Moderate load activity or low load blowdown (Static)                                                    |
      | Fuel Model | Standard | Fuel Model | SB3/203 - High load activity fuel or moderate load blowdown (Static)                                              |
      | Fuel Model | Standard | Fuel Model | SB4/204 - High load blowdown (Static)                                                                             |
      | Fuel Model | Standard | Fuel Model | FB3/3 - Tall grass (Static)                                                                                       |
      | Fuel Model | Standard | Fuel Model | FB4/4 - Chaparral (Static)                                                                                        |
      | Fuel Model | Standard | Fuel Model | FB5/5 - Brush (Static)                                                                                            |
      | Fuel Model | Standard | Fuel Model | FB6/6 - Dormant brush, hardwood slash (Static)                                                                    |
      | Fuel Model | Standard | Fuel Model | FB7/7 - Southern rough (Static)                                                                                   |
      | Fuel Model | Standard | Fuel Model | FB8/8 - Short needle litter (Static)                                                                              |
      | Fuel Model | Standard | Fuel Model | FB9/9 - Long needle or hardwood litter (Static)                                                                   |
