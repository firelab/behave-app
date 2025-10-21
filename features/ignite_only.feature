Feature: Ignite Only Worksheets

  Scenario: Fire Behavior Output Selected
    Given I have started a new Surface Worksheet in Guided Mode
    When I select these outputs Submodule > Group > Output:
      """
      - Fire Behavior > Ignition > Probability of Ignition
      """
    Then the following input Submodule > Groups are displayed:
      """
      - Fuel Moisture > Moisture Input Mode
      - Weather > Air Temperature
      - Weather > Fuel Shading From the Sun
      """
