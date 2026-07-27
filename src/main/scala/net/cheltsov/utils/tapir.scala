package net.cheltsov.utils

import zio.prelude.{Newtype, NonEmptySet}

import shapeless.LowPriority
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.{Codec, CodecFormat}

object tapir {
  implicit def nonEmptySetOptionCodec[T, U](implicit
    codec: Codec[T, U, CodecFormat.TextPlain]
  ): Codec[List[T], Option[NonEmptySet[U]], CodecFormat.TextPlain] =
    Codec
      .list[T, U, CodecFormat.TextPlain]
      .map(NonEmptySet.fromIterableOption(_))(_.toList.flatten)

  implicit def newtypeCodec[A, N <: Newtype[A]](implicit
    convert:  NewtypeConvert[A, N#Type],
    codec:    PlainCodec[A],
    priority: LowPriority
  ): PlainCodec[N#Type] = codec.mapEither(convert.from)(convert.to)
}
