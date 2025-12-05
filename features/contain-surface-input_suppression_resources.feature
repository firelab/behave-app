Feature: Contain & Surface Input - Suppression -> Resources

  Scenario: Resources is displayed
    Given I have started a new Surface & Contain Worksheet in Guided Mode
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Suppression -> Contain Mode -> Add Resources
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Suppression -> Resources
      """