Feature: Crown & Surface Input - Weather -> Wind and slope are -> Wind Direction

  Scenario: Wind Direction is displayed
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Weather -> Wind and slope are -> Not Aligned (Wind is >30° from upslope).
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Weather -> Wind and slope are -> Wind Direction
      """