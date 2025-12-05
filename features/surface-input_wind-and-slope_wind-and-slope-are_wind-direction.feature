Feature: Surface Input - Wind and Slope -> Wind and slope are -> Wind Direction

  Scenario: Wind Direction is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Direction Mode -> Heading
      -- Fire Behavior -> Direction Mode -> Heading, Flanking, Backing
      """
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Wind and Slope -> Wind and slope are -> Not Aligned (Wind is >30° from upslope).
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Wind and Slope -> Wind and slope are -> Wind Direction
      """