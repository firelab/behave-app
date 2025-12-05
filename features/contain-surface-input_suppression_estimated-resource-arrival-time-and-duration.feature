Feature: Contain & Surface Input - Suppression -> Estimated Resource Arrival Time and Duration

  Scenario: Estimated Resource Arrival Time and Duration is displayed
    Given I have started a new Surface & Contain Worksheet in Guided Mode
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Suppression -> Contain Mode -> Calculate Minimum Production Rate Only
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Suppression -> Estimated Resource Arrival Time and Duration
      """