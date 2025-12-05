Feature: Surface Input - Fuel Model -> Standard -> Fuel Model

  Scenario: Fuel Model is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are NOT selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Wind-Driven Surface Fire
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Model -> Standard -> Fuel Model
      """