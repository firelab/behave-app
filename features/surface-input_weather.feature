Feature: Surface Input - Weather

  Scenario: Weather is displayed when Probability of Ignition is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Ignition -> Probability of Ignition
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Weather
      """
      
  Scenario: Weather is displayed when Probability of Ignition is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Fire Behavior -> Ignition -> Probability of Ignition
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Weather
      """
