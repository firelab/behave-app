Feature: Mortality & Surface Input - Scorch -> Fire -> Surface Fire Flame Length

  Scenario: Surface Fire Flame Length is displayed
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Tree Characteristics -> Mortality Tree Species -> Abies amabilis / ABAM (Pacific silver fir)
      -- Scorch -> Fire -> Flame Length
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Scorch -> Fire -> Surface Fire Flame Length
      """