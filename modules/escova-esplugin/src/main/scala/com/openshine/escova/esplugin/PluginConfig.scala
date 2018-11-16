package com.openshine.escova.esplugin

import com.openshine.escova.CostConfig
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

/** Helper in order to load this configuration instance in Java, since Ficus is
  * implemented with Scala macros.
  *
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object PluginConfig {
  def toNative(cfg: Config): CostConfig = {
    cfg.as[CostConfig]
  }
}
