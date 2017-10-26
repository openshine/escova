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
      extends Monad[ComplexityMeasure]
        with Applicative[ComplexityMeasure] {

      def unit(a: Int) = SimpleComplexityMeasure(0)

      def bind[A, B](ma: ComplexityMeasure[A],
                     bf: (A) => ComplexityMeasure[B]):
      ComplexityMeasure[B] = ma.flatMap(bf)

      override def liftA2[A, B](f: (A, A) => B): (ComplexityMeasure[A],
        ComplexityMeasure[A]) => ComplexityMeasure[B] = {
        (a: ComplexityMeasure[A], b: ComplexityMeasure[A]) =>
          unit(f(a.value, b.value))
      }

      override def unit[A](a: A): ComplexityMeasure[A] =
        SimpleComplexityMeasure(a)
    }

  }

}
