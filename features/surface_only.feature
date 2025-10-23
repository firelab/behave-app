Feature: Surface Only Output Selection Conditionals
  
As a user when I create a new worksheet and have Selected Surface Only in the Module Selection Page and I've chosen certain outputs in the outputs page I expect certain inputs to be available to me in the inputs page.

  Scenario: Heading Rate of Spread is Selected
    Given I have started a new Surface Worksheet in Guided Mode
    When I select these outputs Submodule > Group > Output:
      """
      -- Fire Behavior > Direction Mode > Heading
      -- Fire Behavior > Surface Fire > Rate of Spread
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Fuel Model > Standard > Fuel Model
      -- Fuel Moisture > Moisture Input Mode
      -- Wind and Slope > Wind and slope are
      -- Wind and Slope > Slope
      """

      # - Wind and Slope > Wind measured at: @kenny this fails because Wind measured at: has a
      # - trailing space in the dom and (extract-submodule-groups) trims this.
      
  Scenario: Fire Area is Selected
    Given I have started a new Surface Worksheet in Guided Mode
    When I select these outputs Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Fire Area
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Size > Elapsed Time
      """
      
  Scenario: Fire Perimeter is Selected
    Given I have started a new Surface Worksheet in Guided Mode
    When I select these outputs Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Fire Perimeter
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Size > Elapsed Time
      """

  Scenario: Spread Distance is Selected
    Given I have started a new Surface Worksheet in Guided Mode
    When I select these outputs Submodule > Group > Output:
      """
      -- Size > Surface - Fire Size > Spread Distance
      """
    Then the following input Submodule > Groups are displayed:
      """
      -- Size > Elapsed Time
      """

  # Scenario: Length-to-Width Output Selected
  #   Given I have started a Surface & Mortality Worksheet in Guided Mode
  #   When I select the output "Length-to-Width Ratio" in the "Size" submodule
  #   Then the following input Submodule > Groups are displayed:
  #     """
  #     - Fuel Model
  #     - Fuel Moisture > Moisture Input Mode
  #     - Wind and Slope > Wind Speed
  #     - Wind and Slope > Wind and Slope are:
  #     - Wind and Slope > Slope
  #     """

  # Scenario: Size Outputs Selected
  #   Given I have started a Surface Worksheet in Guided Mode
  #   When The size outputs below are selected:
  #   - Size > Fire Area
  #   - Size > Fire Perimeter
  #   - Size > Spread Distance
  #   Then the following input Submodule > Groups are displayed:
  #     """
  #     - Fuel Model
  #     - Fuel Moisture > Moisture Input Mode
  #     - Wind and Slope > Wind Speed
  #     - Wind and Slope > Wind and Slope are:
  #     - Wind and Slope > Slope
  #     - Size > Elapsed Time
  #     """

  # Scenario: Size Outputs Selected
  #   Given I have started a Surface Worksheet in Guided Mode
  #   Then the following outputs are displayed:
  #     """
  #     - Spot -> Burning Pile
  #     - Spot -> Wind-Driven Surface Fire
      # """
    # Then  and  should be the only two options under Maximum Spotting Distance

    # Given I have started a Surface Worksheet
    # When Any outputs are selected, other than Burning Pile
    # Then Maximum Spotting Distance: Burning Pile should be deactivated

    # Given I have started a Surface Worksheet
    # When Burning Pile is selected from Maximum Spotting Distance
    # Then All other outputs should be deactivated and the ONLY inputs should
    # be:
    # - Wind and Slope
    # - Wind Measured at:
    # - Midflame should be deactivate and 20-Foot autoselected
    # - Wind Speed
    # - *No WAF*
    # - *No Wind and Slope are*
    # - *No Slope*
    # - Spot
    # - Downwind Canopy Cover
    # - Downwind Canopy Height
    # - Flame Height (from a Burning Bile)
    # - Topography
    # - Ridge-to-Valley Elevation Difference
    # - Ridge-to-Valley Horizontal Distance (Dependent on Elevation
    # Difference)
    # - Spotting Source Location (Dependent on Elevation Difference)

    # Given I have started a Surface Worksheet
    # When Fire Behavior or Size is selected with Wind-Driven Surface Fire
    # from Spot
    # Then Fuel Model should be replaced with Wind Driven Fuel Models which
    # only contain grass fuel models

    # Given I have started a Surface Worksheet
    # When Fire Behavior or Size is selected with Wind-Driven Surface Fire
    # from Spot
    # Then Surface Fire Flame Length should come from Surface and should not
    # be an input

    # Given I have started a Surface Worksheet
    # When When Wind-Drive Surface fire is not run with Fire Behavior
    # Then Only the inputs below are required
    # - Wind and Slope
    # - Wind Measured at:
    # - Midflame should be deactivate and 20-Foot auto-selected
    # - Wind Speed
    # - *No WAF*
    # - *No Wind and Slope are*
    # - *No Slope*
    # - Spot
    # - Downwind Canopy Cover
    # - Downwind Canopy Height
    # - Topography
    # - Ridge-to-Valley Elevation Difference
    # - Ridge-to-Valley Horizontal Distance (Dependent on Elevation
    # Difference)
    # - Spotting Source Location (Dependent on Elevation Difference)
    # - *Surface Fire Flame Length*

    # Given I have started a Surface Worksheet
    # When 0 is entered into Ridge-to-Valley Elevation Difference
    # Then Ridge-to-Valley Horizontal Distance and Spotting Source Location
    # should not be available inputs

    # Given I have started a Surface Worksheet
    # When A value greater than 0 is entered into Ridge-to-Valley Elevation
    # Difference
    # Then Ridge-to-Valley Horizontal Distance and Spotting Source Location
    # should be required inputs

    # Given I have started a Surface Worksheet
    # When Direction of Interest is selected from Direction Mode
    # Then The Wind/Slope/Spread Diagram should be automatically output on
    # the Run Results

    # Given I have started a Surface Worksheet
    # When Direction of Interest is selected from Direction Mode
    # Then The Direction of Spread should be automatically output on the Run
    # Results. The Direction of Spread should be consisted with the Direction
    # Mode selected (Heading or Heading Flanking Backing)

    # Given I have started a Surface Worksheet
    # When Heading OR Heading, Backing, Flanking, AND Wind and Slope are not
    # aligned
    # Then The Wind/Slope/Spread Diagram should be automatically output on
    # the Run Results

    # Given I have started a Surface Worksheet
    # When Heading OR Heading, Backing, Flanking, AND Wind and Slope are not
    # aligned
    # Then The Direction of Spread should be automatically ouput on the Run
    # Results. The Direction of Spread should be consisted with the Direction
    # Mode selected (Heading or Heading Flanking Backing)

    # Given I have started a Surface Worksheet
    # When Maximum Spotting DIstance from a Burning Pile is run
    # Then Firebrand Height from a Burning Pile should be automatically
    # output
    # * Surface and Crown

    # Given I have started a Surface and Crown Worksheet
    # When Surface and Crown are run together
    # Then Heading should be automatically run for Direction Mode. *It should not be automatically selected because the user may not run Fire Behavior.*

    # Given I have started a Surface and Crown Worksheet
    # When Any output is selected, other than a Spot model
    # Then Fire Type should be automatically selected as an output but it
    # should not shown on the worksheet

    # Given I have started a Surface and Crown Worksheet
    # When Fire behavior has a selected output (RoS, FL, or FI)
    # Then The following Submodules w/inputs are the ONLY required inputs
    # - Fuel Model
    # - Fuel Moisture
    # - Moisture Input Mode
    # - Appropriate Moisture Inputs
    # - Wind and Slope
    # - Wind Measured at:
    # - Midflame should be deactivate and 20-Foot autoselected
    # - Wind Speed
    # - WAF
    # - Wind and Slope are:
    # - Slope
    # - Calculations Options
    # - Fuel Moisture
    # - Foliar Moisture
    # - Canopy Fuel
    # - Canopy Base Height
    # - Canopy Bulk Density
    # - Canopy Height

    # Given I have started a Surface and Crown Worksheet
    # When Length-to-Width Ratio output in the Size submodule is selected
    # Then The following Submodules w/inputs are the ONLY required inputs
    # - Fuel Model
    # - Fuel Moisture
    # - Moisture Input Mode
    # - Appropriate Moisture Inputs
    # - Wind and Slope
    # - Wind Measured at:
    # - Midflame should be deactivate and 20-Foot autoselected
    # - Wind Speed
    # - WAF
    # - WAF(if applicable)
    # - Wind and Slope are:
    # - Slope
    # - Fuel Moisture
    # - Foliar Moisture
    # - Calculations Options
    # - Canopy Fuel
    # - Canopy Base Height
    # - Canopy Bulk Density
    # - Canopy Height

    # Given I have started a Surface and Crown Worksheet
    # When Any Fire Type output are selected
    # Then The following Submodules w/inputs are the ONLY required inputs
    # - Fuel Model
    # - Fuel Moisture
    # - Moisture Input Mode
    # - Appropriate Moisture Inputs
    # - Wind and Slope
    # - Wind Measured at:
    # - Midflame should be deactivate and 20-Foot autoselected
    # - Wind Speed
    # - WAF
    # - WAF(if applicable)
    # - Wind and Slope are:
    # - Slope
    # - Fuel Moisture
    # - Foliar Moisture
    # - Calculations Options
    # - Canopy Fuel
    # - Canopy Base Height
    # - Canopy Bulk Density
    # - Canopy Height

    # Given I have started a Surface and Crown Worksheet
    # When The size outputs below are selected:
    # - Fire Area
    # - Fire Perimeter
    # - Spread Distance
    # - (**Exclude Length-to-Width Ratio)
    # Then The following Submodules w/inputs are the ONLY required inputs
    # - Fuel Model
    # - Fuel Moisture
    # - Moisture Input Mode
    # - Appropriate Moisture Inputs
    # - Wind and Slope
    # - Wind Measured at:
    # - Wind Speed
    # - WAF(if applicable)
    # - Wind and Slope are:
    # - Slope
    # - Size
    # - Elapsed Time

    # Given I have started a Surface and Crown Worksheet
    # When Surface and Crown are run together
    # Then Only Torching Trees and Active Crown fire should be available options under Maximum Spotting Distance

    # Given I have started a Surface and Crown Worksheet
    # When Surface and Crown are run together
    # Then Torching Tree and Active Crown fire should be able to both be run under Maximum Spotting Distance

    # Given I have started a Surface and Crown Worksheet
    # When Active Crown Fire is selected as an out, *WITHOUT Fire Behavior*
    # Then The inputs below are required
    # - Canopy Fuel
    # - Canopy Height
    # - Wind and Slope
    # - Wind Measured at:
    # - Midflame should be deactivate and 20-Foot auto-selected
    # - Wind Speed
    # - *No WAF*
    # - *No Wind and Slope are*
    # - *No Slope*
    # - Spot
    # - Topography
    # - Ridge-to-Valley Elevation Difference
    # - Ridge-to-Valley Horizontal Distance (Dependent on Elevation
    # Difference)
    # - Spotting Source Location (Dependent on Elevation Difference)
    # - *Active Crown Fire Flame Length*

    # Given I have started a Surface and Crown Worksheet
    # When Active Crown Fire is selected as an output with Fire Behavior
    # Then The inputs below are required
    # - Canopy Fuel
    # - Canopy Height
    # - Wind and Slope
    # - Wind Measured at:
    # - Midflame should be deactivate and 20-Foot auto-selected
    # - Wind Speed
    # - *No WAF*
    # - *No Wind and Slope are*
    # - *No Slope*
    # - Spot
    # - Topography
    # - Ridge-to-Valley Elevation Difference
    # - Ridge-to-Valley Horizontal Distance (Dependent on Elevation
    # Difference)
    # - Spotting Source Location (Dependent on Elevation Difference)
    # - *Active Crown Fire Flame Length*

    # Given I have started a Surface and Crown Worksheet
    # When Fire Behavior or Size is selected with Active Crown Fire from Spot
    # Then Active Crown Fire Flame Length should come from Crown and should not be an input
    # * Surface and Contain

    # Given I have started a Surface and Contain Worksheet
    # Then Spot should not be on worksheet

    # Given I have started a Surface and Contain Worksheet
    # Then Surface Fire Behavior and Size conditionals should be treated the same as Surface being run alone

    # Given I have started a Surface and Contain Worksheet
    # Then Heading in Fire Behavior's Direction Mode should be the only option available. Heading, Backing, and Flanking, and DIrection of Interest should be deactivated
    # * Surface and Mortality

    # Given I have started a Surface and Mortality Worksheet
    # Then Heading and Heading Flanking, Backing in Fire Behavior's Direction Mode should be the only options available.
    # And: Direction of Interest should be deactivated

    # Given I have started a Surface and Mortality Worksheet
    # Then There should be no Size output module
    # And: There should be no Size input submodules

    # Given I have started a Surface and Mortality Worksheet
    # Then There should be no mortality output submodules because all outputs are automated based on species selected and their PoM equation

    # Given I have started a Surface and Mortality Worksheet
    # Then There should be no mortality output submodules because all outputs are automated based on species selected and their PoM equation

    # Given I have started a Surface and Mortality Worksheet
    # Then There should be no mortality output submodules because all outputs are automated based on species selected and their PoM equation

    # Given I have started a Surface and Mortality Worksheet
    # Then Flame Length is needed to calculate PoM so Surface Fire Behavior conditionals should be used to calculate Flame Length

    # Given I have started a Surface and Mortality Worksheet
    # Then PoM equation used should be based on the Mortality tree species used, see [[https://sig-gis.atlassian.net/browse/BHP1-839?atlOrigin=eyJpIjoiZTdjZDg4MDNhYTBlNDE2NDljZTRhZTEzNThlNDI5NzgiLCJwIjoiaiJ9][BHP1-839]]

    # Given I have started a Surface and Mortality Worksheet
    # Then DBH and Mortality Tree species are both required user inputs, regardless of PoM equation

    # Given I have started a Surface and Mortality Worksheet
    # Then Probability of Mortality is automatically calculated

    # Given I have started a Surface and Mortality Worksheet
    # Then Mortality Outputs should match the format in [[https://sig-gis.atlassian.net/browse/BHP1-926?atlOrigin=eyJpIjoiYTQwNWFjMmExZDE5NGNjYWI3NDYxNTNjY2MwMmIwMTAiLCJwIjoiaiJ9][ticket]] and [[https://usfs.box.com/s/u6uknqwzt751top5awzn0am4u4s8dkj3][table]]

    # Given I have started a Surface and Crown Worksheet
    # When The PoM equation used is Crown Scorch
    # Then The user inputs below are required
    # - Air Temp
    # - MidFlame Windspeed or (20ft or 10m x WAF = Midflame Windspeed)
    # - Canopy Height
    # - Crown Ratio

    # Given I have started a Surface and Crown Worksheet
    # When The PoM equation used is Crown Scorch
    # Then The calculated Flame Length needs to be used to calculate Scorch Height

    # Given I have started a Surface and Crown Worksheet
    # When The PoM equation used is Bark Char
    # Then The calculated Flame Length is used to calculate Bark Char Height, Flame Length/1.8

    # Given I have started a Surface and Crown Worksheet
    # When The PoM equation used is Crown Scorch
    # Then Automated outputs that should be calculated including:
    # - Crown Length Scorched
    # - Crown Volume Scorched
    # - Scorch Height

    # Given I have started a Surface and Crown Worksheet
    # When The PoM equation used is Bark Char
    # Then Bark Char Height is an automated output that should be calculated include
