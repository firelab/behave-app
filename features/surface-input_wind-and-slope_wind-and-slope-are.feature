@core
Feature: Surface Input - Wind and Slope -> Wind and slope are

  @core
  Scenario Outline: Wind and slope are is displayed with Surface outputs
    Given I have started a new Surface Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule      | group              |
      | Wind and Slope | Wind and slope are |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group               | value                      |
      | Fire Behavior | Direction Mode      | Heading, Flanking, Backing |
      | Fire Behavior | Direction Mode      | Direction of Interest      |
      | Fire Behavior | Direction Mode      | Heading                    |
      | Size          | Surface - Fire Size | Spread Distance            |
      | Size          | Surface - Fire Size | Fire Area                  |
      | Size          | Surface - Fire Size | Fire Perimeter             |
      | Size          | Surface - Fire Size | Length-to-Width Ratio      |
  
Scenario Outline: Wind and slope are is displayed with Crown outputs
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule      | group              |
      | Wind and Slope | Wind and slope are |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group                    | value                               |
      | Fire Behavior | Fire Behavior            | Rate of Spread                      |
      | Fire Behavior | Fire Behavior            | Flame Length                        |
      | Fire Behavior | Fire Behavior            | Fireline Intensity                  |
      | Size          | Crown - Fire Size        | Fire Area                           |
      | Size          | Crown - Fire Size        | Fire Perimeter                      |
      | Size          | Crown - Fire Size        | Spread Distance                     |
      | Fire Type     | Transition to Crown Fire | Critical Surface Fireline Intensity |
      | Fire Type     | Transition to Crown Fire | Transition Ratio                    |
      | Fire Type     | Transition to Crown Fire | Critical Surface Flame Length       |
      | Fire Type     | Active Crown Fire        | Critical Crown Rate of Spread       |
      | Fire Type     | Active Crown Fire        | Active Ratio                        |
