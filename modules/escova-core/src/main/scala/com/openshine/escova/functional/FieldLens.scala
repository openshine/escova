package com.openshine.escova.functional

/**
  * This is here in case we want to ditch Monocle
  *
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
class FieldLens[+C, F](instance: C,
                       getter: (C) => (F),
                       setter: (C) => (F) => (C)
                      ) {

  private val _instance: C = instance

  def apply: F = getter(_instance)

  def apply(value: F): C = setter(_instance)(value)

}

object FieldLens {
  def apply[C, F](instance: C)(
    getter: (C) => (F),
    setter: (C) => (F) => (C)
  ): FieldLens[C, F] =
    new FieldLens[C, F](instance, getter, setter)
}
