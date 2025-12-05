Feature: Surface Input - Fuel Moisture -> Dead, Live Herb, and Live Woody Categories

  Scenario: Dead, Live Herb, and Live Woody Categories is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Surface Fire -> Flame Length
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Fuel Moisture -> Moisture Input Mode -> Dead, Live Herb, and Live Woody Categories
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Fuel Moisture -> Dead, Live Herb, and Live Woody Categories
      """