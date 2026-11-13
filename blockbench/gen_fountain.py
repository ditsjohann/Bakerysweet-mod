"""Chocolate fountain: a single-block, round, three-tier fountain. Chocolate itself is drawn by the renderer."""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from round_model import Model, BLOCK_DISPLAY

OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/models")


def fountain(path):
    m = Model({"ceramic": "bakery:block/fountain/ceramic", "gold": "bakery:block/fountain/gold",
               "particle": "bakery:block/fountain/ceramic"}, display=BLOCK_DISPLAY)
    # Gold foot and a wide ceramic basin with a gold lip
    m.disc("foot", 6.2, 0, 1.2, "gold")
    m.disc("basin_base", 7.4, 1.2, 2.2, "ceramic")
    m.disc("basin_floor", 7.0, 2.2, 3.0, "ceramic")
    m.ring("basin_wall", 7.9, 6.6, 2.2, 4.6, "ceramic", segments=16)
    m.ring("basin_lip", 8.0, 6.9, 4.6, 5.1, "gold", segments=16)
    # Pedestal
    m.disc("pedestal", 2.6, 3.0, 5.5, "gold")
    m.disc("pedestal_neck", 1.8, 5.5, 7.0, "ceramic")
    # Lower tier
    m.disc("tier1_under", 4.0, 7.0, 7.8, "ceramic")
    m.disc("tier1", 5.6, 7.8, 8.8, "ceramic")
    m.ring("tier1_lip", 6.0, 5.2, 8.8, 9.4, "gold")
    m.disc("column1", 1.2, 8.8, 13.4, "gold")
    # Middle tier
    m.disc("tier2_under", 2.6, 13.0, 13.6, "ceramic")
    m.disc("tier2", 3.9, 13.6, 14.4, "ceramic")
    m.ring("tier2_lip", 4.3, 3.6, 14.4, 14.9, "gold")
    m.disc("column2", 0.9, 14.4, 18.2, "gold")
    # Top tier and crown
    m.disc("tier3", 2.4, 18.0, 18.8, "ceramic")
    m.ring("tier3_lip", 2.7, 2.1, 18.8, 19.2, "gold", segments=8)
    m.disc("crown", 1.2, 18.8, 20.6, "gold")
    m.dome("crown_top", 1.2, 20.6, 1.4, "gold", steps=2)
    m.save(path)


fountain(os.path.join(OUT, "block/chocolate_fountain.json"))
fountain(os.path.join(OUT, "block/chocolate_fountain_active.json"))
