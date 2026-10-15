@core
Feature: Surface Input - Wind and Slope -> Wind and slope are -> Wind Direction (from upslope)

  @core
  Scenario Outline: Wind Direction (from upslope) is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When this output path is selected <submodule> : <group> : <value>
    When these input paths are entered
      | submodule      | group              | value                                    |
      | Wind and Slope | Wind and slope are | Not Aligned (Wind is >30° from upslope). |
    Then the following input paths are displayed:
      | submodule      | group              | value                         |
      | Wind and Slope | Wind and slope are | Wind Direction (from upslope) |
    Examples: This scenario is repeated for each of these rows
      | submodule     | group          | value                      |
      | Fire Behavior | Direction Mode | Heading                    |
      | Fire Behavior | Direction Mode | Heading, Flanking, Backing |
      | Fire Behavior | Direction Mode | Direction of Interest      |
