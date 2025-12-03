Feature: Crown & Surface Input - Spot > Torching Trees

  Scenario: Torching Trees is displayed when Torching Trees is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Torching Trees
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Spot > Torching Trees
      """