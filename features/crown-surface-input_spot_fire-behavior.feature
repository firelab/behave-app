Feature: Crown & Surface Input - Spot -> Fire Behavior

  Scenario: Fire Behavior is displayed when Active Crown Fire is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Active Crown Fire
      """
    When these outputs are NOT selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Fire Behavior -> Rate of Spread
      -- Fire Behavior -> Fire Behavior -> Fireline Intensity
      -- Fire Behavior -> Fire Behavior -> Flame Length
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Spot -> Fire Behavior
      """