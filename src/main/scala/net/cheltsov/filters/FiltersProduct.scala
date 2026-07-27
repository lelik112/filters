package net.cheltsov.filters

import net.cheltsov.model.{AnalyticOperation, AnalyticRichOperation}

import shapeless._
import shapeless.ops.hlist.Prepend

trait FiltersProduct[F] {
  type Repr <: HList
  def toRepr(f: F): Repr
  def accountApplier: FilterApplier[Repr]
}

trait ModelFiltersProduct[F, O <: AnalyticOperation] extends FiltersProduct[F] {
  def modelApplier: ModelFilterApplier[Repr, O]
}

trait RichFiltersProduct[F, O <: AnalyticOperation, RO <: AnalyticRichOperation] extends ModelFiltersProduct[F, O] {
  def richApplier: RichFilterApplier[Repr, O, RO]
}

object ModelFiltersProduct {

  type ModelAux[F, R <: HList, O <: AnalyticOperation] = ModelFiltersProduct[F, O] {
    type Repr = R
  }

  implicit def modelFilterIsProduct[T <: ModelFilterType, O <: AnalyticOperation](implicit
    aa: FilterApplier[Filter[T] :: HNil],
    ma: ModelFilterApplier[Filter[T] :: HNil, O]
  ): ModelAux[Filter[T], Filter[T] :: HNil, O] =
    new ModelFiltersProduct[Filter[T], O] {
      type Repr = Filter[T] :: HNil
      def toRepr(f: Filter[T]): Repr = f :: HNil
      val accountApplier: FilterApplier[Repr]       = aa
      val modelApplier: ModelFilterApplier[Repr, O] = ma
    }

  implicit def modelCaseClassFlattened[F, R0 <: HList, R <: HList, O <: AnalyticOperation](implicit
    gen: Generic.Aux[F, R0],
    fl:  FlattenModel.Aux[R0, O, R],
    aa:  FilterApplier[R],
    ma:  ModelFilterApplier[R, O]
  ): ModelAux[F, R, O] =
    new ModelFiltersProduct[F, O] {
      type Repr = R
      def toRepr(f: F): R = fl(gen.to(f))
      val accountApplier: FilterApplier[R]       = aa
      val modelApplier: ModelFilterApplier[R, O] = ma
    }

  implicit def modelCombine[A, B, RA <: HList, RB <: HList, R <: HList, O <: AnalyticOperation](implicit
    fa: ModelAux[A, RA, O],
    fb: ModelAux[B, RB, O],
    pr: Prepend.Aux[RA, RB, R],
    aa: FilterApplier[R],
    ma: ModelFilterApplier[R, O]
  ): ModelAux[(A, B), R, O] =
    new ModelFiltersProduct[(A, B), O] {
      type Repr = R
      def toRepr(ab: (A, B)): R = pr(fa.toRepr(ab._1), fb.toRepr(ab._2))
      val accountApplier: FilterApplier[R]       = aa
      val modelApplier: ModelFilterApplier[R, O] = ma
    }

  sealed trait FlattenModel[L <: HList, O <: AnalyticOperation] {
    type Out <: HList
    def apply(l: L): Out
  }

  object FlattenModel {
    type Aux[L <: HList, O <: AnalyticOperation, Out0 <: HList] = FlattenModel[L, O] { type Out = Out0 }

    implicit def hNil[O <: AnalyticOperation]: Aux[HNil, O, HNil] =
      new FlattenModel[HNil, O] {
        type Out = HNil
        def apply(l: HNil): HNil = HNil
      }

    implicit def hCons[H, T <: HList, O <: AnalyticOperation, AH <: HList, AT <: HList](implicit
      hp: ModelFiltersProduct[H, O] { type Repr = AH },
      tp: Aux[T, O, AT]
    ): Aux[H :: T, O, AH :: AT] =
      new FlattenModel[H :: T, O] {
        type Out = AH :: AT
        def apply(l: H :: T): Out = hp.toRepr(l.head) :: tp(l.tail)
      }
  }
}

object RichFiltersProduct {

  type RichAux[F, R <: HList, O <: AnalyticOperation, RO <: AnalyticRichOperation] = RichFiltersProduct[F, O, RO] {
    type Repr = R
  }

  implicit def richFilterIsProduct[T <: FilterType, O <: AnalyticOperation, RO <: AnalyticRichOperation](implicit
    aa: FilterApplier[Filter[T] :: HNil],
    ra: RichFilterApplier[Filter[T] :: HNil, O, RO]
  ): RichAux[Filter[T], Filter[T] :: HNil, O, RO] =
    new RichFiltersProduct[Filter[T], O, RO] {
      type Repr = Filter[T] :: HNil
      def toRepr(f: Filter[T]): Repr = f :: HNil
      val accountApplier: FilterApplier[Repr]         = aa
      val modelApplier: ModelFilterApplier[Repr, O]   = ra
      val richApplier: RichFilterApplier[Repr, O, RO] = ra
    }

  implicit def richCaseClassFlattened[F, R0 <: HList, R <: HList, O <: AnalyticOperation, RO <: AnalyticRichOperation](
    implicit
    gen: Generic.Aux[F, R0],
    fl:  FlattenRich.Aux[R0, O, RO, R],
    aa:  FilterApplier[R],
    ra:  RichFilterApplier[R, O, RO]
  ): RichAux[F, R, O, RO] =
    new RichFiltersProduct[F, O, RO] {
      type Repr = R
      def toRepr(f: F): R = fl(gen.to(f))
      val accountApplier: FilterApplier[R]         = aa
      val modelApplier: ModelFilterApplier[R, O]   = ra
      val richApplier: RichFilterApplier[R, O, RO] = ra
    }

  implicit def richCombine[
    A,
    B,
    RA <: HList,
    RB <: HList,
    R <: HList,
    O <: AnalyticOperation,
    RO <: AnalyticRichOperation
  ](implicit
    fa: RichAux[A, RA, O, RO],
    fb: RichAux[B, RB, O, RO],
    pr: Prepend.Aux[RA, RB, R],
    aa: FilterApplier[R],
    ra: RichFilterApplier[R, O, RO]
  ): RichAux[(A, B), R, O, RO] =
    new RichFiltersProduct[(A, B), O, RO] {
      type Repr = R
      def toRepr(ab: (A, B)): R = pr(fa.toRepr(ab._1), fb.toRepr(ab._2))
      val accountApplier: FilterApplier[R]         = aa
      val modelApplier: ModelFilterApplier[R, O]   = ra
      val richApplier: RichFilterApplier[R, O, RO] = ra
    }

  sealed trait FlattenRich[L <: HList, O <: AnalyticOperation, RO <: AnalyticRichOperation] {
    type Out <: HList
    def apply(l: L): Out
  }

  object FlattenRich {
    type Aux[L <: HList, O <: AnalyticOperation, RO <: AnalyticRichOperation, Out0 <: HList] = FlattenRich[L, O, RO] {
      type Out = Out0
    }

    implicit def hNil[O <: AnalyticOperation, RO <: AnalyticRichOperation]: Aux[HNil, O, RO, HNil] =
      new FlattenRich[HNil, O, RO] {
        type Out = HNil
        def apply(l: HNil): HNil = HNil
      }

    implicit def hCons[H, T <: HList, O <: AnalyticOperation, RO <: AnalyticRichOperation, AH <: HList, AT <: HList](
      implicit
      hp: RichFiltersProduct[H, O, RO] { type Repr = AH },
      tp: Aux[T, O, RO, AT]
    ): Aux[H :: T, O, RO, AH :: AT] =
      new FlattenRich[H :: T, O, RO] {
        type Out = AH :: AT
        def apply(l: H :: T): Out = hp.toRepr(l.head) :: tp(l.tail)
      }
  }
}
