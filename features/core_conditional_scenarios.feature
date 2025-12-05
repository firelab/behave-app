@core
Feature: Core Conditional Scenarios

  # 1. Input Group should appear when output group is selected
  @core
  Scenario: Heading Rate of Spread is Selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Heading
      -- Fire Behavior -> Surface Fire -> Rate of Spread
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Model -> Standard -> Fuel Model
      -- Fuel Moisture -> Moisture Input Mode
      -- Wind and Slope -> Wind and slope are
      -- Wind and Slope -> Slope
      """

      # 5. Input Submodule should appear when Output Group is selected
  @core
  Scenario: Elapsed Time should Appear in the Inputs when Fire Area Output is Selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Surface - Fire Size -> Fire Area
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Size -> Elapsed Time
      """

      # 4. Input Group should appear when some Output Groups are selected and some Output Groups are NOT selected
  @core
  Scenario: Spot Submodule Should Appear when
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Wind-Driven Surface Fire
      """
    And these outputs are NOT selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Heading
      -- Fire Behavior -> Direction Mode -> Heading, Flanking, Backing
      -- Fire Behavior -> Direction Mode -> Direction of Interest
      """

    Then the following input Submodule -> Groups are displayed:
      """
      -- Spot
      """

      # 2. Input Submodule should NOT appear when output group is selected
  @core
  Scenario: Spot Submodule Should Not Appear when Direction Mode: Heading is Selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Heading
      """
    Then the following input Submodule -> Groups are NOT displayed:
      """
      -- Spot
      """

      # 3. Input Group should appear when another Input Group has a value is selected
  @core
  Scenario: By Size Class Appears when Moisture Input Mode is Individual Size Class
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Rate of Spread
      """
    And these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class
      """

      # - Input is displayed when Input is Selected
  @core
  Scenario: 10-h, 100-h, and Live Herbaceous Fuel Moisture Inputs Appears when Fuel Model: FB1/2 is Selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Rate of Spread
      """
    And these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Model -> Standard -> Fuel Model -> FB2/2 - Timber grass and understory (Static)
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      -- Fuel Moisture -> By Size Class -> 100-h Fuel Moisture
      -- Fuel Moisture -> By Size Class -> Live Herbaceous Fuel Moisture
      """
