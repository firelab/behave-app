Feature: Mortality & Surface Input - Scorch -> Wind Adjustment Factor

  Scenario: Wind Adjustment Factor is displayed
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Tree Characteristics -> Mortality Tree Species -> Abies amabilis / ABAM (Pacific silver fir)
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Scorch -> Wind Adjustment Factor
      """