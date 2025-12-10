@core
Feature: Surface Input - Wind and Slope -> Wind Adjustment Factor

  @core
  Scenario Outline: Wind Adjustment Factor is displayed when
    Given I have started a new Surface Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    When these input paths are entered
      | submodule      | group             | value   |
      | Wind and Slope | Wind Measured at: | 20-Foot |
    Then the following input paths are displayed:
      | submodule      | group                  |
      | Wind and Slope | Wind Adjustment Factor |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group                     | value                      |
      | Fire Behavior | Direction Mode            | Heading                    |
      | Fire Behavior | Direction Mode            | Heading, Flanking, Backing |
      | Fire Behavior | Direction Mode            | Direction of Interest      |
      | Spot          | Maximum Spotting Distance | Wind-Driven Surface Fire   |

  @core
  Scenario Outline: Wind Adjustment Factor is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    When these input paths are entered
      | submodule      | group             | value    |
      | Wind and Slope | Wind Measured at: | 10-Meter |
    Then the following input paths are displayed:
      | submodule      | group                  |
      | Wind and Slope | Wind Adjustment Factor |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group                     | value                      |
      | Fire Behavior | Direction Mode            | Heading                    |
      | Fire Behavior | Direction Mode            | Heading, Flanking, Backing |
      | Fire Behavior | Direction Mode            | Direction of Interest      |
      | Spot          | Maximum Spotting Distance | Wind-Driven Surface Fire   |

  @core
  Scenario Outline: Wind Adjustment Factor is displayed
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    When these input paths are entered
      | submodule      | group             | value   |
      | Wind and Slope | Wind Measured at: | 20-Foot |
    Then the following input paths are displayed:
      | submodule      | group                  |
      | Wind and Slope | Wind Adjustment Factor |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group                    | value                               |
      | Fire Behavior | Fire Behavior            | Rate of Spread                      |
      | Fire Behavior | Fire Behavior            | Flame Length                        |
      | Fire Behavior | Fire Behavior            | Fireline Intensity                  |
      | Fire Type     | Active Crown Fire        | Active Ratio                        |
      | Fire Type     | Active Crown Fire        | Critical Crown Rate of Spread       |
      | Fire Type     | Transition to Crown Fire | Critical Surface Fireline Intensity |
      | Fire Type     | Transition to Crown Fire | Critical Surface Flame Length       |
      | Fire Type     | Transition to Crown Fire | Transition Ratio                    |
      | Size          | Crown - Fire Size        | Fire Area                           |
      | Size          | Crown - Fire Size        | Fire Perimeter                      |
      | Fire Behavior | Fire Behavior            | Rate of Spread                      |
      | Size          | Crown - Fire Size        | Spread Distance                     |

  @core
  Scenario Outline: Wind Adjustment Factor is displayed
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    When these input paths are entered
      | submodule      | group             | value    |
      | Wind and Slope | Wind Measured at: | 10-Meter |
    Then the following input paths are displayed:
      | submodule      | group                  |
      | Wind and Slope | Wind Adjustment Factor |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group                    | value                               |
      | Fire Behavior | Fire Behavior            | Rate of Spread                      |
      | Fire Behavior | Fire Behavior            | Flame Length                        |
      | Fire Behavior | Fire Behavior            | Fireline Intensity                  |
      | Fire Type     | Active Crown Fire        | Active Ratio                        |
      | Fire Type     | Active Crown Fire        | Critical Crown Rate of Spread       |
      | Fire Type     | Transition to Crown Fire | Critical Surface Fireline Intensity |
      | Fire Type     | Transition to Crown Fire | Critical Surface Flame Length       |
      | Fire Type     | Transition to Crown Fire | Transition Ratio                    |
      | Size          | Crown - Fire Size        | Fire Area                           |
      | Size          | Crown - Fire Size        | Fire Perimeter                      |
      | Fire Behavior | Fire Behavior            | Rate of Spread                      |
      | Size          | Crown - Fire Size        | Spread Distance                     |
