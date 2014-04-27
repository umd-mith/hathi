package edu.umd.mith

import scalaz.{ \/, ValidationNel }

package object util {
  type OrError[A] = Throwable \/ A
  type Valid[A] = ValidationNel[Throwable, A]
}

