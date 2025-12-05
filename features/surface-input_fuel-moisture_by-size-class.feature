Feature: Surface Input - Fuel Moisture -> By Size Class

  Scenario: By Size Class is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Surface Fire -> Flame Length
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Individual Size Class
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> By Size Class
      """