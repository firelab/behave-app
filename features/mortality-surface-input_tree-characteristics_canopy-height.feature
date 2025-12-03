Feature: Mortality & Surface Input - Tree Characteristics > Canopy Height

  Scenario: Canopy Height is displayed
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When these inputs are entered Submodule > Group > Input:
      """
      -- Tree Characteristics > Mortality Tree Species > Abies amabilis / ABAM (Pacific silver fir)
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Tree Characteristics > Canopy Height
      """