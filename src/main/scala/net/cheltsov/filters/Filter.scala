package net.cheltsov.filters

final case class Filter[T <: FilterType](value: Option[T#Content]) extends AnyVal {
  def isEmpty: Boolean  = value.isEmpty
  def nonEmpty: Boolean = value.nonEmpty
}

object Filter {
  def empty[T <: FilterType]: Filter[T] = Filter(None)
}
