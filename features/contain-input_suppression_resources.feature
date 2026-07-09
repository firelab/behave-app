@core
Feature: Contain & Surface Input - Suppression -> Resources

  @core
  Scenario: Resources is displayed
    Given I have started a new Surface & Contain Worksheet in Guided Mode
    When these input paths are entered
      | submodule   | group        | value         |
      | Suppression | Contain Mode | Add Resources |
    Then the following input paths are displayed:
      | submodule   | group     |
      | Suppression | Resources |