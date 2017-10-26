package com.openshine.escova.functional

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
sealed trait ComplexityMeasure[A] {
  def value: A

  def map(f: A => A)(
    implicit a: Monad[ComplexityMeasure]
  ): ComplexityMeasure[A] = a.unit(f(value))

  def flatMap[B](f: A => ComplexityMeasure[B]):
  ComplexityMeasure[B] = f(value)

}

case class SimpleComplexityMeasure[A](_value: A) extends
  ComplexityMeasure[A] {

  override def value: A = _value
}
