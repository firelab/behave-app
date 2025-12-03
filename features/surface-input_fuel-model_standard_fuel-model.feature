Feature: Surface Input - Fuel Model > Standard > Fuel Model

  Scenario: Fuel Model is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Flame Length
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model > Standard > Fuel Model
      """

  Scenario: Fuel Model is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Fireline Intensity
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model > Standard > Fuel Model
      """

  Scenario: Fuel Model is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Rate of Spread
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model > Standard > Fuel Model
      """

  Scenario: Fuel Model is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Spread Distance
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model > Standard > Fuel Model
      """

  Scenario: Fuel Model is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Fire Area
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model > Standard > Fuel Model
      """

  Scenario: Fuel Model is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Fire Perimeter
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model > Standard > Fuel Model
      """

  Scenario: Fuel Model is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Length-to-Width Ratio
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model > Standard > Fuel Model
      """