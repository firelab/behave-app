Feature: Surface Input - Wind and Slope -> Wind Speed

  Scenario: Wind Speed is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Heading
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind Measured at: -> Midflame (Eye Level)
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind Speed
      """