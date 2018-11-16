package com.openshine.escova

import scala.language.higherKinds

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
package object functional {

  trait Monad[M[_]] {
    def unit[A](a: A): M[A]

    def bind[A, B](ma: M[A], bf: A => M[B]): M[B]
  }

  trait Applicative[F[_]] {
    def liftA2[A, B](f: (A, A) => B): (F[A], F[A]) => F[B]
  }

  object monads {

    implicit object ComplexityMeasureMonad
        extends Monad[CostMeasure]
        with Applicative[CostMeasure] {

      def unit(a: Int) = SimpleCostMeasure(0)

      def bind[A, B](ma: CostMeasure[A],
                     bf: (A) => CostMeasure[B]): CostMeasure[B] = ma.flatMap(bf)

      override def liftA2[A, B](f: (A, A) => B)
        : (CostMeasure[A], CostMeasure[A]) => CostMeasure[B] = {
        (a: CostMeasure[A], b: CostMeasure[A]) =>
          unit(f(a.value, b.value))
      }

      override def unit[A](a: A): CostMeasure[A] =
        SimpleCostMeasure(a)
    }

  }

}
