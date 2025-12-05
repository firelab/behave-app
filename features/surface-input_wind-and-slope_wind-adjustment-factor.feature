Feature: Surface Input - Wind and Slope -> Wind Adjustment Factor

  Scenario: Wind Adjustment Factor is displayed when Heading is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Heading
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 20-Foot
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Heading, Flanking, Backing is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Heading, Flanking, Backing
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 20-Foot
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Direction of Interest is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Direction of Interest
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 20-Foot
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Wind-Driven Surface Fire is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Wind-Driven Surface Fire
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 20-Foot
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """