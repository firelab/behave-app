Feature: Surface Input - Wind and Slope

  Scenario Outline: Wind and Slope is displayed with Surface outputs
    Given I have started a new Surface Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule      |
      | Wind and Slope |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group                     | value                      |
      | Fire Behavior | Direction Mode            | Heading                    |
      | Fire Behavior | Direction Mode            | Direction of Interest      |
      | Fire Behavior | Direction Mode            | Heading, Flanking, Backing |
      | Fire Behavior | Surface Fire              | Rate of Spread             |
      | Fire Behavior | Surface Fire              | Flame Length               |
      | Fire Behavior | Surface Fire              | Fireline Intensity         |
      | Spot          | Maximum Spotting Distance | Burning Pile               |
      | Spot          | Maximum Spotting Distance | Wind-Driven Surface Fire   |
      | Size          | Surface - Fire Size       | Fire Area                  |
      | Size          | Surface - Fire Size       | Fire Perimeter             |
      | Size          | Surface - Fire Size       | Length-to-Width Ratio      |
      | Size          | Surface - Fire Size       | Spread Distance            |

  Scenario Outline: Wind and Slope is displayed with Crown outputs
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule      |
      | Wind and Slope |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group                     | value                               |
      | Fire Behavior | Fire Behavior             | Rate of Spread                      |
      | Fire Behavior | Fire Behavior             | Flame Length                        |
      | Fire Behavior | Fire Behavior             | Fireline Intensity                  |
      | Fire Type     | Active Crown Fire         | Active Ratio                        |
      | Fire Type     | Active Crown Fire         | Critical Crown Rate of Spread       |
      | Fire Type     | Transition to Crown Fire  | Critical Surface Fireline Intensity |
      | Fire Type     | Transition to Crown Fire  | Critical Surface Flame Length       |
      | Fire Type     | Transition to Crown Fire  | Transition Ratio                    |
      | Size          | Crown - Fire Size         | Fire Area                           |
      | Size          | Crown - Fire Size         | Fire Perimeter                      |
      | Size          | Crown - Fire Size         | Spread Distance                     |
      | Spot          | Maximum Spotting Distance | Torching Trees                      |
      | Spot          | Maximum Spotting Distance | Active Crown Fire                   |
