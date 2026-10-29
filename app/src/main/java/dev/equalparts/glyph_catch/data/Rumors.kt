package dev.equalparts.glyph_catch.data

import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R

/**
 * A hint about how a legendary or mythical Pokémon appears. It becomes readable once the
 * Pokédex reaches [unlockAt] entries.
 */
data class Rumor(val speciesId: Int, val unlockAt: Int, @param:StringRes val textRes: Int)

object Rumors {
    val all = listOf(
        Rumor(145, 20, R.string.rumor_zapdos),
        Rumor(144, 30, R.string.rumor_articuno),
        Rumor(146, 30, R.string.rumor_moltres),
        Rumor(150, 60, R.string.rumor_mewtwo),
        Rumor(243, 80, R.string.rumor_raikou),
        Rumor(244, 100, R.string.rumor_entei),
        Rumor(245, 100, R.string.rumor_suicune),
        Rumor(249, 160, R.string.rumor_lugia),
        Rumor(250, 120, R.string.rumor_ho_oh),
        Rumor(377, 180, R.string.rumor_regirock),
        Rumor(378, 180, R.string.rumor_regice),
        Rumor(379, 180, R.string.rumor_registeel),
        Rumor(380, 200, R.string.rumor_latias),
        Rumor(381, 200, R.string.rumor_latios),
        Rumor(251, 220, R.string.rumor_celebi),
        Rumor(385, 230, R.string.rumor_jirachi),
        Rumor(480, 230, R.string.rumor_uxie),
        Rumor(481, 230, R.string.rumor_mesprit),
        Rumor(482, 230, R.string.rumor_azelf),
        Rumor(483, 250, R.string.rumor_dialga),
        Rumor(484, 250, R.string.rumor_palkia),
        Rumor(151, 280, R.string.rumor_mew),
        Rumor(382, 280, R.string.rumor_kyogre),
        Rumor(383, 280, R.string.rumor_groudon),
        Rumor(384, 300, R.string.rumor_rayquaza),
        Rumor(486, 300, R.string.rumor_regigigas),
        Rumor(485, 330, R.string.rumor_heatran),
        Rumor(386, 360, R.string.rumor_deoxys),
        Rumor(488, 360, R.string.rumor_cresselia),
        Rumor(491, 370, R.string.rumor_darkrai),
        Rumor(490, 380, R.string.rumor_manaphy),
        Rumor(489, 390, R.string.rumor_phione),
        Rumor(492, 380, R.string.rumor_shaymin),
        Rumor(487, 400, R.string.rumor_giratina),
        Rumor(494, 400, R.string.rumor_victini),
        Rumor(638, 430, R.string.rumor_cobalion),
        Rumor(639, 430, R.string.rumor_terrakion),
        Rumor(640, 430, R.string.rumor_virizion),
        Rumor(647, 440, R.string.rumor_keldeo),
        Rumor(641, 450, R.string.rumor_tornadus),
        Rumor(642, 450, R.string.rumor_thundurus),
        Rumor(645, 460, R.string.rumor_landorus),
        Rumor(493, 470, R.string.rumor_arceus),
        Rumor(643, 480, R.string.rumor_reshiram),
        Rumor(644, 480, R.string.rumor_zekrom),
        Rumor(646, 490, R.string.rumor_kyurem),
        Rumor(648, 530, R.string.rumor_meloetta),
        Rumor(649, 580, R.string.rumor_genesect)
    ).sortedBy { it.unlockAt }
}
