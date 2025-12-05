Feature: Crown & Surface Input - Size

  Scenario: Size is displayed when Fire Perimeter is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Fire Perimeter
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Size
      """

  Scenario: Size is displayed when Fire Area is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Fire Area
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Size
      """

  Scenario: Size is displayed when Spread Distance is selected
    Given I have started a new Surface & Crown Worksheet in Guided Mode
    When these outputs are selected Submodule -> Group -> Output:
      """
      -- Size -> Crown - Fire Size -> Spread Distance
      """
    Then the following input Submodule -> Groups are displayed:
      """
      -- Size
      """