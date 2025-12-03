Feature: Surface Input - Fuel Moisture > Dead, Live Herb, and Live Woody Categories > Dead Fuel Moisture

  Scenario: Dead Fuel Moisture is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Spread Distance
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Model > Standard > Fuel Model > FB1/1 - Short grass (Static)
      -- Fuel Moisture > Moisture Input Mode > Dead, Live Herb, and Live Woody Categories
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Dead, Live Herb, and Live Woody Categories > Dead Fuel Moisture
      """

  Scenario: Dead Fuel Moisture is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Fire Area
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Model > Standard > Fuel Model > FB1/1 - Short grass (Static)
      -- Fuel Moisture > Moisture Input Mode > Dead, Live Herb, and Live Woody Categories
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Dead, Live Herb, and Live Woody Categories > Dead Fuel Moisture
      """

  Scenario: Dead Fuel Moisture is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Fire Perimeter
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Model > Standard > Fuel Model > FB1/1 - Short grass (Static)
      -- Fuel Moisture > Moisture Input Mode > Dead, Live Herb, and Live Woody Categories
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Dead, Live Herb, and Live Woody Categories > Dead Fuel Moisture
      """

  Scenario: Dead Fuel Moisture is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Flame Length
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Model > Standard > Fuel Model > FB1/1 - Short grass (Static)
      -- Fuel Moisture > Moisture Input Mode > Dead, Live Herb, and Live Woody Categories
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Dead, Live Herb, and Live Woody Categories > Dead Fuel Moisture
      """

  Scenario: Dead Fuel Moisture is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Rate of Spread
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Model > Standard > Fuel Model > FB1/1 - Short grass (Static)
      -- Fuel Moisture > Moisture Input Mode > Dead, Live Herb, and Live Woody Categories
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Dead, Live Herb, and Live Woody Categories > Dead Fuel Moisture
      """

  Scenario: Dead Fuel Moisture is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Length-to-Width Ratio
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Model > Standard > Fuel Model > FB1/1 - Short grass (Static)
      -- Fuel Moisture > Moisture Input Mode > Dead, Live Herb, and Live Woody Categories
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Dead, Live Herb, and Live Woody Categories > Dead Fuel Moisture
      """

  Scenario: Dead Fuel Moisture is displayed
    Given I have started a new Surface Worksheet in Guided Mode
    When these outputs are selected Submodule > Group > Output:
      """
      -- Fire Behavior > Surface Fire > Fireline Intensity
      """
    When these outputs are NOT selected Submodule > Group > Output:
      """
      -- Spot > Maximum Spotting Distance > Wind-Driven Surface Fire
      """
    When these inputs are entered Submodule > Group > Input:
      """
      -- Fuel Model > Standard > Fuel Model > FB1/1 - Short grass (Static)
      -- Fuel Moisture > Moisture Input Mode > Dead, Live Herb, and Live Woody Categories
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Moisture > Dead, Live Herb, and Live Woody Categories > Dead Fuel Moisture
      """