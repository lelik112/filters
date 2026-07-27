package net.cheltsov.utils

import zio.prelude.Newtype
import zio.{Task, ZIO}

import shapeless.Witness

trait NewtypeConvert[A, B] {
  def from(value: A): Either[String, B]
  def to(value: B): A

  final def fromZIO(value: A): Task[B] =
    ZIO.fromEither(from(value).left.map(new IllegalArgumentException(_)))
}

object NewtypeConvert {
  implicit def instance[A, N <: Newtype[A]](implicit
    ev:      N <:< Newtype[A], // is needed somehow
    witness: Witness.Aux[N]
  ): NewtypeConvert[A, N#Type] = new NewtypeConvert[A, N#Type] {
    override def from(value: A): Either[String, N#Type] =
      witness.value.make(value).toEitherWith(_.head)

    override def to(value: N#Type): A =
      witness.value.unwrap(value.asInstanceOf[witness.value.Type]) // scalafix:ok
  }
}
