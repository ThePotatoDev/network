package gg.tater.shared.island.setting.model

import gg.tater.shared.island.IslandService
import me.lucko.helper.terminable.module.TerminableModule

abstract class IslandSettingHandler(val service: IslandService) : TerminableModule {
}