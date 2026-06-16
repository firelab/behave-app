@core
Feature: Surface Input - Fuel Model -> Standard -> Fuel Model

  @core
  Scenario: Fuel Model is displayed when Wind-Driven Surface Fire (Grass Only) is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group                     | value                                 |
      | Fire Behavior | Surface Fire              | Flame Length                          |
      | Spot          | Maximum Spotting Distance | Wind-Driven Surface Fire (Grass Only) |
    Then the following input paths are displayed:
      | submodule  | group    | value      |
      | Fuel Model | Standard | Fuel Model |