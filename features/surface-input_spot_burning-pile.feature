Feature: Surface Input - Spot -> Burning Pile

  Scenario: Burning Pile is displayed when Burning Pile is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Burning Pile
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Spot -> Burning Pile
      """

  #Investigate
  # Scenario: Burning Pile is displayed when Firebrand Height from a Burning Pile is selected
  #   Given I have started a new Surface Worksheet in Guided Mode
  #   When these outputs are selected Submodule -> Group -> Output:
  #     """
  #     -- Spot -> Burning Pile -> Firebrand Height from a Burning Pile
  #     """
  #   Then the following input Submodule -> Groups are displayed:
  #     """
  #     -- Spot -> Burning Pile
  #     """
