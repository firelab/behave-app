Feature: Crown & Surface Input - Weather -> Wind Adjustment Factor -> Wind Adjustment Factor

  Scenario: Wind Adjustment Factor is displayed
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Weather -> Wind Adjustment Factor -> User Input
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Weather -> Wind Adjustment Factor -> Wind Adjustment Factor
      """