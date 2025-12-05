Feature: Surface Input - Fuel Moisture -> By Size Class -> 10-h Fuel Moisture

  Scenario: 10-h Fuel Moisture is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Surface Fire -> Flame Length
      """
    When these outputs are NOT selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Wind-Driven Surface Fire
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Model -> Standard -> Fuel Model -> FB10/10 - Timber litter & understory (Static)
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class -> 10-h Fuel Moisture
      """