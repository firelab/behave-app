@core
Feature: Surface Input - Weather

  @core
  Scenario Outline: Weather is displayed with Surface outputs
    Given I have started a new Surface Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule |
      | Weather   |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group    | value                   |
      | Fire Behavior | Ignition | Probability of Ignition |

  @core
  Scenario Outline: Weather is displayed with Surface & Crown outputs
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule |
      | Weather   |

    Examples: This scenario is repeated for each of these rows
      | submodule     | group    | value                   |
      | Fire Behavior | Ignition | Probability of Ignition |