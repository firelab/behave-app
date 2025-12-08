Feature: Crown & Surface Input - Wind and Slope -> Wind Adjustment Factor

  Background:
    Given I have started a new Surface & Crown Worksheet in Guided Mode

  Scenario: Wind Adjustment Factor is displayed when Rate of Spread is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Rate of Spread
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Flame Length is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Flame Length
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Fireline Intensity is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Fireline Intensity
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Active Ratio is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Active Crown Fire -> Active Ratio
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Critical Crown Rate of Spread is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Active Crown Fire -> Critical Crown Rate of Spread
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Critical Surface Fireline Intensity is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Transition to Crown Fire -> Critical Surface Fireline Intensity
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Critical Surface Flame Length is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Transition to Crown Fire -> Critical Surface Flame Length
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Transition Ratio is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Transition to Crown Fire -> Transition Ratio
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Fire Area is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Fire Area
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Fire Perimeter is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Fire Perimeter
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Length-to-Width Ratio is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Rate of Spread
      -- Size -> Crown - Fire Size -> Length-to-Width Ratio
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """

  Scenario: Wind Adjustment Factor is displayed when Spread Distance is selected
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Spread Distance
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> 10-Meter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Adjustment Factor
      """
