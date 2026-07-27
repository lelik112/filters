package net.cheltsov.filters

import shapeless._
import sttp.tapir.EndpointInput

trait FilterInput[F] {
  def input(filter: F): EndpointInput[F]
}

object FilterInput {

  def input[F](filter: F)(implicit input: FilterInput[F]): EndpointInput[F] = input.input(filter)

  implicit def filterInstance[T <: ApiFilterType](implicit w: Witness.Aux[T]): FilterInput[Filter[T]] =
    _ =>
      w.value.input
        .asInstanceOf[EndpointInput[Option[T#Content]]] // scalafix:ok
        .map(Filter[T](_))(_.value)

  implicit def lastInstance[H](implicit lastInput: FilterInput[H]): FilterInput[H :: HNil] =
    (last: H :: HNil) => lastInput.input(last.head).map(_ :: HNil)(_.head)

  implicit def hListInstance[H, L <: HList](implicit
    headInput: FilterInput[H],
    tailInput: FilterInput[L]
  ): FilterInput[H :: L] = { case head :: tail =>
    (headInput.input(head) and tailInput.input(tail))
      .map(t => t._1 :: t._2) { case head :: tail => head -> tail }
  }
}
