Feature: Surface Input - Spot -> Surface Fire Flame Length

  Scenario: Surface Fire Flame Length is displayed when Wind-Driven Surface Fire is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Wind-Driven Surface Fire
      """
    When these outputs are NOT selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Heading
      -- Fire Behavior -> Direction Mode -> Direction of Interest
      -- Fire Behavior -> Direction Mode -> Heading, Flanking, Backing
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Spot -> Surface Fire Flame Length
      """