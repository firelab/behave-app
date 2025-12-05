Feature: Crown & Surface Input - Wind and Slope

  Scenario: Wind and Slope is displayed when Rate of Spread is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Rate of Spread
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Flame Length is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Flame Length
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Fireline Intensity is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Fireline Intensity
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Active Ratio is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Active Crown Fire -> Active Ratio
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Critical Crown Rate of Spread is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Active Crown Fire -> Critical Crown Rate of Spread
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Critical Surface Fireline Intensity is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Transition to Crown Fire -> Critical Surface Fireline Intensity
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Critical Surface Flame Length is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Transition to Crown Fire -> Critical Surface Flame Length
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Transition Ratio is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Transition to Crown Fire -> Transition Ratio
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Fire Area is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Fire Area
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Fire Perimeter is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Fire Perimeter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Spread Distance is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Spread Distance
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Torching Trees is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Torching Trees
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """

  Scenario: Wind and Slope is displayed when Active Crown Fire is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Active Crown Fire
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope
      """