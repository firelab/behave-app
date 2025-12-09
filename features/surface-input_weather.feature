Feature: Surface Input - Weather

  Scenario: Weather is displayed when Probability of Ignition is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group    | value                   |
      | Fire Behavior | Ignition | Probability of Ignition |
    Then the following input paths are displayed:
      | submodule |
      | Weather   |
      
  Scenario: Weather is displayed when Probability of Ignition is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group    | value                   |
      | Fire Behavior | Ignition | Probability of Ignition |
    Then the following input paths are displayed:
      | submodule |
      | Weather   |
