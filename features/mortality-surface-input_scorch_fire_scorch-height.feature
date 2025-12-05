Feature: Mortality & Surface Input - Scorch -> Fire -> Scorch Height

  Scenario: Scorch Height is displayed
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When these inputs are entered Submodule -> Group -> Input:
      """
      -- Tree Characteristics -> Mortality Tree Species -> Abies amabilis / ABAM (Pacific silver fir)
      -- Scorch -> Fire -> Scorch Height
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Scorch -> Fire -> Scorch Height
      """