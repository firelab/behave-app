@core
Feature: Surface Input - Fuel Model -> Standard -> Fuel Model

  @core
  Scenario: Fuel Model is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group        | value          |
      | Fire Behavior | Surface Fire | Rate of Spread |
    When these output paths are NOT selected
      | submodule | group                     | value                    |
      | Spot      | Maximum Spotting Distance | Wind-Driven Surface Fire |
    Then the following input paths are displayed:
      | submodule  | group    | value      |
      | Fuel Model | Standard | Fuel Model |
