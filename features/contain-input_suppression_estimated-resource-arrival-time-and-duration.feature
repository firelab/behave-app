@core
Feature: Contain & Surface Input - Suppression -> Estimated Resource Arrival Time and Duration

  @core
  Scenario: Estimated Resource Arrival Time and Duration is displayed
    Given I have started a new Surface & Contain Worksheet in Guided Mode
    When these input paths are entered
      | submodule   | group        | value                                  |
      | Suppression | Contain Mode | Calculate Minimum Production Rate Only |
    Then the following input paths are displayed:
      | submodule   | group                                        |
      | Suppression | Estimated Resource Arrival Time and Duration |
