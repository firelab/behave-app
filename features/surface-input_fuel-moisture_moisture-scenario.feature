Feature: Surface Input - Fuel Moisture > Moisture Scenario

  Scenario: Moisture Scenario is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Flame Length
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Moisture > Moisture Input Mode > Moisture Scenario
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Moisture Scenario
      """

  Scenario: Moisture Scenario is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Fireline Intensity
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Moisture > Moisture Input Mode > Moisture Scenario
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Moisture Scenario
      """

  Scenario: Moisture Scenario is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Rate of Spread
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Moisture > Moisture Input Mode > Moisture Scenario
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Moisture Scenario
      """

  Scenario: Moisture Scenario is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Spread Distance
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Moisture > Moisture Input Mode > Moisture Scenario
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Moisture Scenario
      """

  Scenario: Moisture Scenario is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Fire Area
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Moisture > Moisture Input Mode > Moisture Scenario
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Moisture Scenario
      """

  Scenario: Moisture Scenario is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Fire Perimeter
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Moisture > Moisture Input Mode > Moisture Scenario
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Moisture Scenario
      """

  Scenario: Moisture Scenario is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Length-to-Width Ratio
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Moisture > Moisture Input Mode > Moisture Scenario
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Moisture Scenario
      """

  Scenario: Moisture Scenario is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Ignition > Probability of Ignition
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Moisture > Moisture Input Mode > Moisture Scenario
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Moisture Scenario
      """