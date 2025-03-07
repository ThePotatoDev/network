package gg.tater.core.item

import me.lucko.helper.terminable.module.TerminableModule

interface CustomItemService: TerminableModule {

    companion object {
        const val ROCKET_SHIP_ENGINE_MODEL_ID = 7000
        const val ROCKET_SHIP_GLASS_MODEL_ID = 7001
        const val ROCKET_SHIP_NAV_MODEL_ID = 7002
    }

}