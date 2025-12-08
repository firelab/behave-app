Feature: Crown & Surface Input - Fuel Moisture -> By Size Class -> 10-h Fuel Moisture

  Scenario: 10-h Fuel Moisture is displayed when Rate of Spread is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Rate of Spread
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """

  Scenario: 10-h Fuel Moisture is displayed when Flame Length is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Flame Length
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """

  Scenario: 10-h Fuel Moisture is displayed when Fireline Intensity is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Fireline Intensity
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """

  Scenario: 10-h Fuel Moisture is displayed when Active Ratio is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Active Crown Fire -> Active Ratio
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """

  Scenario: 10-h Fuel Moisture is displayed when Critical Crown Rate of Spread is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Active Crown Fire -> Critical Crown Rate of Spread
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """

  Scenario: 10-h Fuel Moisture is displayed when Critical Surface Fireline Intensity is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Transition to Crown Fire -> Critical Surface Fireline Intensity
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """

  Scenario: 10-h Fuel Moisture is displayed when Critical Surface Flame Length is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Transition to Crown Fire -> Critical Surface Flame Length
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """

  Scenario: 10-h Fuel Moisture is displayed when Transition Ratio is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Type -> Transition to Crown Fire -> Transition Ratio
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """

  Scenario: 10-h Fuel Moisture is displayed when Fire Area is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Fire Area
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """

  Scenario: 10-h Fuel Moisture is displayed when Fire Perimeter is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Fire Perimeter
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """

  Scenario: 10-h Fuel Moisture is displayed when Spread Distance is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Spread Distance
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """
