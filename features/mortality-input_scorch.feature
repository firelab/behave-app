@core
Feature: Mortality & Surface Input - Scorch

  @core
  Scenario Outline: Scorch is displayed with these Tree Species
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When this input path is entered <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule |
      | Scorch    |
    Examples: This scenario is repeated for each of these rows
      | submodule            | group                  | value                                      |
      | Tree Characteristics | Mortality Tree Species | Abies amabilis / ABAM (Pacific silver fir) |
 
  @extended
  Scenario Outline: Scorch is displayed with these Tree Species
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When this input path is entered <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule |
      | Scorch    |
    Examples: This scenario is repeated for each of these rows
      | submodule            | group                  | value                                      |
      | Tree Characteristics | Mortality Tree Species | Abies amabilis / ABAM (Pacific silver fir) |
 
  @extended
  Scenario Outline: Scorch is displayed with these Tree Species (extended)
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When this input path is entered <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule |
      | Scorch    |
    Examples: This scenario is repeated for each of these rows
      | submodule            | group                  | value                                      |
      | Tree Characteristics | Mortality Tree Species | Abies amabilis / ABAM (Pacific silver fir) |
 
  @extended
  Scenario Outline: Scorch is displayed with these Tree Species
    Given I have started a new Surface & Mortality Worksheet in Guided Mode
    When this input path is entered <submodule> : <group> : <value>
    Then the following input paths are displayed:
      | submodule |
      | Scorch    |
    Examples: This scenario is repeated for each of these rows
      | submodule            | group                  | value                                                   |
      | Tree Characteristics | Mortality Tree Species | Abies amabilis / ABAM (Pacific silver fir)              |
      | Tree Characteristics | Mortality Tree Species | Abies balsamea / ABBA (Balsam fir)                      |
      | Tree Characteristics | Mortality Tree Species | Abies concolor / ABCO (White fir)                       |
      | Tree Characteristics | Mortality Tree Species | Abies grandis / ABGR (Grand fir)                        |
      | Tree Characteristics | Mortality Tree Species | Abies lasiocarpa / ABLA (Subalpine fir)                 |
      | Tree Characteristics | Mortality Tree Species | Abies magnifica / ABMA (Red Fir)                        |
      | Tree Characteristics | Mortality Tree Species | Abies procera / ABPR (Noble Fir)                        |
      | Tree Characteristics | Mortality Tree Species | Acer barbatum / ACBA3 (Southern sugar maple)            |
      | Tree Characteristics | Mortality Tree Species | Acer macrophyllum / ACMA3 (Bigleaf maple)               |
      | Tree Characteristics | Mortality Tree Species | Acer negundo / ACNE2 (Boxelder)                         |
      | Tree Characteristics | Mortality Tree Species | Acer nigrum / ACNI5 (Black maple)                       |
      | Tree Characteristics | Mortality Tree Species | Acer pensylvanicum / ACPE (Striped maple)               |
      | Tree Characteristics | Mortality Tree Species | Acer saccharinum / ACSA2 (Silver maple)                 |
      | Tree Characteristics | Mortality Tree Species | Acer saccharum / ACSA3 (Sugar maple)                    |
      | Tree Characteristics | Mortality Tree Species | Acer spicatum / ACSP2 (Mountain maple)                  |
      | Tree Characteristics | Mortality Tree Species | Aesculus flava / AEFL (Yellow buckeye)                  |
      | Tree Characteristics | Mortality Tree Species | Aesculus glabra / AEGL (Ohio buckeye)                   |
      | Tree Characteristics | Mortality Tree Species | Ailanthus altissima / AIAL (Ailanthus)                  |
      | Tree Characteristics | Mortality Tree Species | Alnus rhombifolia / ALRH2 (White alder)                 |
      | Tree Characteristics | Mortality Tree Species | Alnus rubra / ALRU2 (Red alder)                         |
      | Tree Characteristics | Mortality Tree Species | Amelanchier arborea / AMAR3 (Common serviceberry)       |
      | Tree Characteristics | Mortality Tree Species | Arbutus menziesii / ARME (Pacific madrone)              |
      | Tree Characteristics | Mortality Tree Species | Betula alleghaniensis / BEAL2 (Yellow birch)            |
      | Tree Characteristics | Mortality Tree Species | Betula lenta / BELE (Sweet birch)                       |
      | Tree Characteristics | Mortality Tree Species | Betula nigra / BENI (River Birch)                       |
      | Tree Characteristics | Mortality Tree Species | Betula occidentalis / BEOC2 (Water birch)               |
      | Tree Characteristics | Mortality Tree Species | Betula papyrifera / BEPA (Paper birch)                  |
      | Tree Characteristics | Mortality Tree Species | Betula species / BETSPP (Birches)                       |
      | Tree Characteristics | Mortality Tree Species | Carya alba / CAAL27 (Mockernut hickory)                 |
      | Tree Characteristics | Mortality Tree Species | Carpinus caroliniana / CACA18 (American hornbeam)       |
      | Tree Characteristics | Mortality Tree Species | Carya cordiformis / CACOL3 (Bitternut hickory)          |
      | Tree Characteristics | Mortality Tree Species | Castanea dentata / CADE12 (American chestnut)           |
      | Tree Characteristics | Mortality Tree Species | Calocedrus decurrens / CADE27 (Incense - cedar)         |
      | Tree Characteristics | Mortality Tree Species | Carya glabra / CAGL8 (Pignut hickory)                   |
      | Tree Characteristics | Mortality Tree Species | Carya illinoinensis / CAIL2 (Pecan)                     |
      | Tree Characteristics | Mortality Tree Species | Carya laciniosa / CALA21 (Shellbark hickory)            |
      | Tree Characteristics | Mortality Tree Species | Carya ovata / CAOV2 (Shagbark hickory)                  |
      | Tree Characteristics | Mortality Tree Species | Carya species / CARSPP (Hickories)                      |
      | Tree Characteristics | Mortality Tree Species | Carya texana / CATE9 (Black hickory)                    |
      | Tree Characteristics | Mortality Tree Species | Cercis canadensis / CECA4 (Eastern redbud)              |
      | Tree Characteristics | Mortality Tree Species | Celtis laevigata / CELA (Sugarberry)                    |
      | Tree Characteristics | Mortality Tree Species | Celtis occidentalis / CEOC (Common hackberry)           |
      | Tree Characteristics | Mortality Tree Species | Chrysolepis chrysophylla / CHCHC4 (Giant chinkapin)     |
      | Tree Characteristics | Mortality Tree Species | Chamaecyparis lawsoniana / CHLA (PortOrford - cedar)    |
      | Tree Characteristics | Mortality Tree Species | Chamaecyparis nootkatensis / CHNO (Alaska - cedar)      |
      | Tree Characteristics | Mortality Tree Species | Chamaecyparis thyoides / CHTH2 (Atlantic white - cedar) |
      | Tree Characteristics | Mortality Tree Species | Cornus nuttallii / CONU4 (Pacific dogwood)              |
      | Tree Characteristics | Mortality Tree Species | Crataegus species / CRASPP (Hawthorns)                  |
      | Tree Characteristics | Mortality Tree Species | Diospyros virginiana / DIVI5 (Persimmon)                |
      | Tree Characteristics | Mortality Tree Species | Fagus grandifolia / FAGR (American beech)               |
      | Tree Characteristics | Mortality Tree Species | Fraxinus americana / FRAM2 (White ash)                  |
      | Tree Characteristics | Mortality Tree Species | Fraxinus species / FRASPP (Ashes)                       |
      | Tree Characteristics | Mortality Tree Species | Fraxinus nigra / FRNI (Black ash)                       |
      | Tree Characteristics | Mortality Tree Species | Fraxinus pennsylvanica / FRPE (Green ash)               |
      | Tree Characteristics | Mortality Tree Species | Fraxinus profunda / FRPR (Pumpkin ash)                  |
      | Tree Characteristics | Mortality Tree Species | Fraxinus quadrangulata / FRQU (Blue ash)                |
      | Tree Characteristics | Mortality Tree Species | Gleditsia triacanthos / GLTR (Honeylocust)              |
      | Tree Characteristics | Mortality Tree Species | Gordonia lasianthus / GOLA (Loblolly bay)               |
      | Tree Characteristics | Mortality Tree Species | Gymnocladus dioicus / GYDI (Kentucky coffeetree)        |
      | Tree Characteristics | Mortality Tree Species | Halesia species / HALSPP (Silverbells)                  |
      | Tree Characteristics | Mortality Tree Species | Ilex opaca / ILOP (American holly)                      |
      | Tree Characteristics | Mortality Tree Species | Juglans cinerea / JUCI (Butternut)                      |
      | Tree Characteristics | Mortality Tree Species | Juglans nigra / JUNI (Black walnut)                     |
      | Tree Characteristics | Mortality Tree Species | Juniperus occidentalis / JUOC (Western juniper)         |
      | Tree Characteristics | Mortality Tree Species | Juniperus virginiana / JUVI (Eastern redcedar)          |
      | Tree Characteristics | Mortality Tree Species | Larix laricina / LALA (Tamarack)                        |
      | Tree Characteristics | Mortality Tree Species | Larix lyallii / LALY (Subalpine Larch)                  |
      | Tree Characteristics | Mortality Tree Species | Larix occidentalis / LAOC (Western Larch)               |
      | Tree Characteristics | Mortality Tree Species | Lithocarpus densiflorus / LIDE3 (Tanoak)                |
      | Tree Characteristics | Mortality Tree Species | Liquidambar styraciflua / LIST2 (Sweetgum)              |
      | Tree Characteristics | Mortality Tree Species | Liriodendron tulipifera / LITU (Tuliptree)              |
      | Tree Characteristics | Mortality Tree Species | Magnolia acuminata / MAAC (Cucumber - tree)             |
      | Tree Characteristics | Mortality Tree Species | Magnolia grandiflora / MAGR4 (Southern magnolia)        |
      | Tree Characteristics | Mortality Tree Species | Magnolia species / MAGSPP (Magnolias)                   |
      | Tree Characteristics | Mortality Tree Species | Prunus species / MALPRU (cherry and plum species)       |
      | Tree Characteristics | Mortality Tree Species | Malus species / MALSPP (Apples)                         |
      | Tree Characteristics | Mortality Tree Species | Magnolia macrophylla / MAMA2 (Bigleaf magnolia)         |
      | Tree Characteristics | Mortality Tree Species | Maclura pomifera / MAPO (Osage - orange)                |
      | Tree Characteristics | Mortality Tree Species | Magnolia virginiana / MAVI2 (Sweetbay)                  |
      | Tree Characteristics | Mortality Tree Species | Morus alba / MOAL (White mulberry)                      |
      | Tree Characteristics | Mortality Tree Species | Morus species / MORSPP (Mulberries)                     |
      | Tree Characteristics | Mortality Tree Species | Morus rubra / MORU2 (Red mulberry)                      |
      | Tree Characteristics | Mortality Tree Species | Nyssa aquatica / NYAQ2 (Water tupelo)                   |
      | Tree Characteristics | Mortality Tree Species | Nyssa sylvatica / NYBI (Blackgum)                       |
      | Tree Characteristics | Mortality Tree Species | Nyssa ogeche / NYOG (Ogeechee tupelo)                   |
      | Tree Characteristics | Mortality Tree Species | Ostrya virginiana / OSVI (Hophornbeam)                  |
      | Tree Characteristics | Mortality Tree Species | Oxydendrum arboreum / OXAR (Sourwood)                   |
      | Tree Characteristics | Mortality Tree Species | Paulownia tomentosa / PATO2 (Princesstree)              |
      | Tree Characteristics | Mortality Tree Species | Persea borbonia / PEBO (Redbay)                         |
      | Tree Characteristics | Mortality Tree Species | Picea abies / PIAB (Norway spruce)                      |
      | Tree Characteristics | Mortality Tree Species | Pinus albicaulis / PIAL (Whitebark pine)                |
      | Tree Characteristics | Mortality Tree Species | Pinus attenuata / PIAT (Knobcone pine)                  |
      | Tree Characteristics | Mortality Tree Species | Pinus banksiana / PIBA2 (Jack pine)                     |
      | Tree Characteristics | Mortality Tree Species | Pinus clausa / PICL (Sand pine)                         |
      | Tree Characteristics | Mortality Tree Species | Pinus contorta / PICO (Lodgepole pine)                  |
      | Tree Characteristics | Mortality Tree Species | Pinus echinata / PIEC2 (Shortleaf pine)                 |
      | Tree Characteristics | Mortality Tree Species | Pinus elliottii / PIEL (Slash pine)                     |
      | Tree Characteristics | Mortality Tree Species | Picea engelmannii / PIEN (Engelmann spruce)             |
      | Tree Characteristics | Mortality Tree Species | Pinus flexilis / PIFL2 (Limber pine)                    |
      | Tree Characteristics | Mortality Tree Species | Picea glauca / PIGL (White spruce)                      |
      | Tree Characteristics | Mortality Tree Species | Pinus glabra / PIGL2 (Spruce pine)                      |
      | Tree Characteristics | Mortality Tree Species | Pinus jeffreyi / PIJE (Jeffrey pine)                    |
      | Tree Characteristics | Mortality Tree Species | Pinus lambertiana / PILA (Sugar pine)                   |
      | Tree Characteristics | Mortality Tree Species | Picea mariana / PIMA (Black spruce)                     |
      | Tree Characteristics | Mortality Tree Species | Pinus monticola / PIMO3 (Western white pine)            |
      | Tree Characteristics | Mortality Tree Species | Pinus palustris / PIPA2 (Longleaf pine)                 |
      | Tree Characteristics | Mortality Tree Species | Pinus ponderosa / PIPO (Ponderosa pine)                 |
      | Tree Characteristics | Mortality Tree Species | Picea pungens / PIPU (Blue spruce)                      |
      | Tree Characteristics | Mortality Tree Species | Pinus pungens / PIPU5 (Table mountain pine)             |
      | Tree Characteristics | Mortality Tree Species | Pinus resinosa / PIRE (Red pine)                        |
      | Tree Characteristics | Mortality Tree Species | Pinus rigida / PIRI (Pitch pine)                        |
      | Tree Characteristics | Mortality Tree Species | Picea rubens / PIRU (Red spruce)                        |
      | Tree Characteristics | Mortality Tree Species | Pinus sabiniana / PISA2 (Gray pine)                     |
      | Tree Characteristics | Mortality Tree Species | Pinus serotina / PISE (Pond pine)                       |
      | Tree Characteristics | Mortality Tree Species | Picea sitchensis / PISI (Sitka spruce)                  |
      | Tree Characteristics | Mortality Tree Species | Pinus strobus / PIST (Eastern white pine)               |
      | Tree Characteristics | Mortality Tree Species | Pinus sylvestris / PISY (Scots pine)                    |
      | Tree Characteristics | Mortality Tree Species | Pinus taeda / PITA (Loblolly pine)                      |
      | Tree Characteristics | Mortality Tree Species | Pinus virginiana / PIVI2 (Virginia pine)                |
      | Tree Characteristics | Mortality Tree Species | Platanus occidentalis / PLOC (American sycamore)        |
      | Tree Characteristics | Mortality Tree Species | Populus balsamifera / POBA2 (Balsam poplar)             |
      | Tree Characteristics | Mortality Tree Species | Populus grandidentata / POGR4 (Bigtooth aspen)          |
      | Tree Characteristics | Mortality Tree Species | Populus heterophylla / POHE4 (Swamp cottonwood)         |
      | Tree Characteristics | Mortality Tree Species | Populus tremuloides / POTR12 (Quaking aspen)            |
      | Tree Characteristics | Mortality Tree Species | Prunus americana / PRAM (American plum)                 |
      | Tree Characteristics | Mortality Tree Species | Prunus emarginata / PREM (Bitter cherry)                |
      | Tree Characteristics | Mortality Tree Species | Prunus pensylvanica / PRPE2 (Pin cherry)                |
      | Tree Characteristics | Mortality Tree Species | Prunus serotina / PRSE2 (Black cherry)                  |
      | Tree Characteristics | Mortality Tree Species | Prunus virginiana / PRVI (Chokecherry)                  |
      | Tree Characteristics | Mortality Tree Species | Pseudotsuga menziesii / PSME (Douglas - fir)            |
      | Tree Characteristics | Mortality Tree Species | Quercus agrifolia / QUAG (California live oak)          |
      | Tree Characteristics | Mortality Tree Species | Quercus chrysolepis / QUCH2 (Canyon live oak)           |
      | Tree Characteristics | Mortality Tree Species | Quercus douglasii / QUDU (Blue oak)                     |
      | Tree Characteristics | Mortality Tree Species | Quercus ellipsoidalis / QUEL (Northern pin oak)         |
      | Tree Characteristics | Mortality Tree Species | Quercus species / QUESPP (Oaks)                         |
      | Tree Characteristics | Mortality Tree Species | Quercus falcata / QUFA (Southern red oak)               |
      | Tree Characteristics | Mortality Tree Species | Quercus imbricaria / QUIM (Shingle oak)                 |
      | Tree Characteristics | Mortality Tree Species | Quercus incana / QUIN (Bluejack oak)                    |
      | Tree Characteristics | Mortality Tree Species | Quercus laevis / QULA2 (Turkey oak)                     |
      | Tree Characteristics | Mortality Tree Species | Quercus laurifolia / QULA3 (Laurel oak)                 |
      | Tree Characteristics | Mortality Tree Species | Quercus lobata / QULO (Valley oak)                      |
      | Tree Characteristics | Mortality Tree Species | Quercus lyrata / QULY (Overcup oak)                     |
      | Tree Characteristics | Mortality Tree Species | Quercus macrocarpa / QUMA2 (Bur oak)                    |
      | Tree Characteristics | Mortality Tree Species | Quercus michauxii / QUMI (Swamp chestnut oak)           |
      | Tree Characteristics | Mortality Tree Species | Quercus muehlenbergii / QUMU (Chinkapin oak)            |
      | Tree Characteristics | Mortality Tree Species | Quercus nigra / QUNI (Water oak)                        |
      | Tree Characteristics | Mortality Tree Species | Quercus palustris / QUPA2 (Pin oak)                     |
      | Tree Characteristics | Mortality Tree Species | Quercus phellos / QUPH (Willow oak)                     |
      | Tree Characteristics | Mortality Tree Species | Quercus rubra / QURU (Northern red oak)                 |
      | Tree Characteristics | Mortality Tree Species | Quercus shumardii / QUSH (Shumard oak)                  |
      | Tree Characteristics | Mortality Tree Species | Quercus stellata / QUST (Post oak)                      |
      | Tree Characteristics | Mortality Tree Species | Quercus texana / QUTE (Texas red oak)                   |
      | Tree Characteristics | Mortality Tree Species | Quercus virginiana / QUVI (Live oak)                    |
      | Tree Characteristics | Mortality Tree Species | Quercus wislizeni / QUWI2 (Interior live oak)           |
      | Tree Characteristics | Mortality Tree Species | Robinia pseudoacacia / ROPS (Black locust)              |
      | Tree Characteristics | Mortality Tree Species | Salix bebbiana / SABE2 (Bebb willow)                    |
      | Tree Characteristics | Mortality Tree Species | Salix species / SALSPP (Willows)                        |
      | Tree Characteristics | Mortality Tree Species | Salix nigra / SANI (Black willow)                       |
      | Tree Characteristics | Mortality Tree Species | Sorbus americana / SOAM3 (American mountain - ash)      |
      | Tree Characteristics | Mortality Tree Species | Taxodium ascendens / TAAS (Pond cypress)                |
      | Tree Characteristics | Mortality Tree Species | Taxus brevifolia / TABR2 (Pacific yew)                  |
      | Tree Characteristics | Mortality Tree Species | Taxodium distichum / TADI2 (Bald cypress)               |
      | Tree Characteristics | Mortality Tree Species | Thuja occidentalis / THOC2 (arborvitae)                 |
      | Tree Characteristics | Mortality Tree Species | Thuja plicata / THPL (Western redcedar)                 |
      | Tree Characteristics | Mortality Tree Species | Tilia americana / TIAM (American basswood)              |
      | Tree Characteristics | Mortality Tree Species | Tsuga canadensis / TSCA (Eastern hemlock)               |
      | Tree Characteristics | Mortality Tree Species | Tsuga heterophylla / TSHE (Western hemlock)             |
      | Tree Characteristics | Mortality Tree Species | Tsuga mertensiana / TSME (Mountain hemlock)             |
      | Tree Characteristics | Mortality Tree Species | Ulmus alata / ULAL (Winged elm)                         |
      | Tree Characteristics | Mortality Tree Species | Ulmus americana / ULAM (American elm)                   |
      | Tree Characteristics | Mortality Tree Species | Ulmus species / ULMSPP (Elms)                           |
      | Tree Characteristics | Mortality Tree Species | Ulmus pumila / ULPU (Siberian elm)                      |
      | Tree Characteristics | Mortality Tree Species | Ulmus rubra / ULRU (Slippery elm)                       |
      | Tree Characteristics | Mortality Tree Species | Ulmus thomasii / ULTH (Rock elm)                        |
      | Tree Characteristics | Mortality Tree Species | Umbellularia californica / UMCA (California - laurel)   |
