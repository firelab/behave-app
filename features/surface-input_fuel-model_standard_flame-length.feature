Feature: Surface Input - Fuel Model -> Standard -> Flame Length

  Scenario: Flame Length is displayed when Wind-Driven Surface Fire is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Wind-Driven Surface Fire
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Model -> Standard -> Flame Length
      """