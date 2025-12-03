Feature: Surface Input - Fuel Model

  Scenario: Fuel Model is displayed when Flame Length is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Flame Length
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model
      """

  Scenario: Fuel Model is displayed when Fireline Intensity is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Fireline Intensity
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model
      """

  Scenario: Fuel Model is displayed when Rate of Spread is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Rate of Spread
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model
      """

  Scenario: Fuel Model is displayed when Spread Distance is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Spread Distance
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model
      """

  Scenario: Fuel Model is displayed when Fire Area is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Fire Area
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model
      """

  Scenario: Fuel Model is displayed when Fire Perimeter is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Fire Perimeter
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model
      """

  Scenario: Fuel Model is displayed when Length-to-Width Ratio is selected
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Length-to-Width Ratio
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model
      """