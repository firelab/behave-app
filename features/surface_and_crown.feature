Feature: Surface and Crown Worksheets

  Scenario: Probability of Ignition Output Selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
    """
    -- Fire Behavior > Ignition > Probability of Ignition
    """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Moisture Input Mode
      -- Weather > Air Temperature
      -- Weather > Fuel Shading From the Sun 
      """

