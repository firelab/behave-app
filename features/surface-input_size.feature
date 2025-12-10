@core
Feature: Surface Input - Size

  @core
  Scenario Outline: Size is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule |
      | Size      |

    Examples: This scenario is repeated for each of these rows
      | submodule | group             | value           |
      | Size      | Surface - Fire Size | Fire Perimeter  |
      | Size      | Surface - Fire Size | Fire Area       |
      | Size      | Surface - Fire Size | Spread Distance |

  @core
  Scenario Outline: Size is displayed
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule |
      | Size      |

    Examples: This scenario is repeated for each of these rows
      | submodule | group             | value           |
      | Size      | Crown - Fire Size | Fire Perimeter  |
      | Size      | Crown - Fire Size | Fire Area       |
      | Size      | Crown - Fire Size | Spread Distance |
