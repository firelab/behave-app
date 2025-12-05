Feature: Crown & Surface Input - Spot

  Scenario: Spot is displayed when Active Crown Fire is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Active Crown Fire
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Spot
      """

  Scenario: Spot is displayed when Torching Trees is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Torching Trees
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Spot
      """