@core
Feature: Surface Input - Wind and Slope -> Wind and slope are -> Wind Direction (from upslope)

  @core
  Scenario: Wind Direction (from upslope) is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these output paths are selected
      | submodule     | group          | value                      |
      | Fire Behavior | Direction Mode | Heading                    |
      | Fire Behavior | Direction Mode | Heading, Flanking, Backing |
    When these input paths are entered
      | submodule      | group              | value                                    |
      | Wind and Slope | Wind and slope are | Not Aligned (Wind is >30° from upslope). |
    Then the following input paths are displayed:
      | submodule      | group              | value                         |
      | Wind and Slope | Wind and slope are | Wind Direction (from upslope) |