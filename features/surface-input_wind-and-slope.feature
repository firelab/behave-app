Feature: Surface Input - Wind and Slope

  Scenario: Wind and Slope is displayed when Heading is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Heading
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Direction of Interest is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Direction of Interest
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Heading, Flanking, Backing is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Heading, Flanking, Backing
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Rate of Spread is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Surface Fire -> Rate of Spread
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Flame Length is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Surface Fire -> Flame Length
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Fireline Intensity is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Surface Fire -> Fireline Intensity
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Burning Pile is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Burning Pile
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Wind-Driven Surface Fire is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Wind-Driven Surface Fire
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Fire Area is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Surface - Fire Size -> Fire Area
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Fire Perimeter is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Surface - Fire Size -> Fire Perimeter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Length-to-Width Ratio is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Surface - Fire Size -> Length-to-Width Ratio
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Spread Distance is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Surface - Fire Size -> Spread Distance
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """