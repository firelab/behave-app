Feature: Surface Input - Fuel Moisture -> All Aggregate

  Scenario: All Aggregate is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Surface Fire -> Flame Length
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> All Aggregate
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> All Aggregate
      """