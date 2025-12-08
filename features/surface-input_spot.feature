Feature: Surface Input - Spot

  #investigate
  # Scenario: Spot is displayed when Firebrand Height from a Burning Pile is selected
  #   Given I have started a new Surface Worksheet in Guided Mode
  #   When these outputs are selected Submodule -> Group -> Output:
  #     """
  #     -- Spot -> Burning Pile -> Firebrand Height from a Burning Pile
  #     """
  #   Then the following input Submodule -> Groups are displayed:
  #     """
  #     -- Spot
  #     """

  Scenario: Spot is displayed when Burning Pile is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Burning Pile
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Spot
      """

  Scenario: Spot is displayed when Wind-Driven Surface Fire is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Spot -> Maximum Spotting Distance -> Wind-Driven Surface Fire
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Spot
      """
