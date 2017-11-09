package com.openshine.escova.functional

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
sealed trait CostMeasure[A] {
  def value: A

  def map(f: A => A)(
    implicit a: Monad[CostMeasure]
  ): CostMeasure[A] = a.unit(f(value))

  def flatMap[B](f: A => CostMeasure[B]):
  CostMeasure[B] = f(value)

}

case class SimpleCostMeasure[A](_value: A) extends
  CostMeasure[A] {

  override def value: A = _value
}
