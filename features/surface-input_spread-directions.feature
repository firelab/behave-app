Feature: Surface Input - Spread Directions

  Scenario: Spread Directions is displayed when Direction of Interest is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Direction Mode > Direction of Interest
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Spread Directions
      """