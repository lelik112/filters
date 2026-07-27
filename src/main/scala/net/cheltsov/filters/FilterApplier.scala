package net.cheltsov.filters

import zio.ZIO.ifZIO
import zio.metrics.Metric
import zio.{UIO, ZIO}

import net.cheltsov.model.{Account, AnalyticOperation, AnalyticRichOperation, Operation, RichOperation}

import shapeless._

trait FilterApplier[F] {
  def apply(filter: F, account: Account): UIO[Boolean]
}

trait ModelFilterApplier[F, O <: AnalyticOperation] {
  def apply(filter: F, model: O): UIO[Boolean]
}

trait RichFilterApplier[F, O <: AnalyticOperation, RO <: AnalyticRichOperation] extends ModelFilterApplier[F, O] {
  def apply(filter: F, rich: RO): UIO[Boolean]
}

object FilterApplier {

  private val Counter      = Metric.counter("filters_applied_total")
  private val TypeLabel    = "type"
  private val ApplierLabel = "applier"
  private val ResultLabel  = "result"

  private[filters] def incrementFalse(filterType: FilterType, applier: String): UIO[Unit] =
    Counter
      .tagged(TypeLabel, filterType.entryName)
      .tagged(ApplierLabel, applier)
      .tagged(ResultLabel, "false")
      .increment

  private[filters] def incrementTrue(applier: String): UIO[Unit] =
    Counter
      .tagged(TypeLabel, "None")
      .tagged(ApplierLabel, applier)
      .tagged(ResultLabel, "true")
      .increment

  implicit def accountFilterInstance[T <: FilterType](implicit
    w: Witness.Aux[T]
  ): FilterApplier[Filter[T]] =
    new FilterApplier[Filter[T]] {
      override def apply(filter: Filter[T], account: Account): UIO[Boolean] =
        (w.value, filter.value) match {

          case (t: AccountFilterType, Some(value)) =>
            val result =
              (t.accountFilter _).asInstanceOf[T#Content => Account => Boolean](value)(account) // scalafix:ok

            incrementFalse(t, "account").unless(result).as(result)

          case _ =>
            ZIO.succeed(true)
        }
    }

  implicit val accountHNilInstance: FilterApplier[HNil] =
    new FilterApplier[HNil] {
      override def apply(filter: HNil, account: Account): UIO[Boolean] = incrementTrue("account").as(true)
    }

  implicit def accountHListInstance[H, L <: HList](implicit
    headApplier: FilterApplier[H],
    tailApplier: FilterApplier[L]
  ): FilterApplier[H :: L] = new FilterApplier[H :: L] {
    override def apply(filter: H :: L, account: Account): UIO[Boolean] =
      filter match {
        case head :: tail => ifZIO(headApplier(head, account))(tailApplier(tail, account), ZIO.succeed(false))
      }
  }

}

object ModelFilterApplier {
  implicit def analyticModelFilterInstance[T <: AnalyticModelFilterType](implicit
    w: Witness.Aux[T]
  ): ModelFilterApplier[Filter[T], AnalyticOperation] =
    new ModelFilterApplier[Filter[T], AnalyticOperation] {
      override def apply(filter: Filter[T], model: AnalyticOperation): UIO[Boolean] =
        (w.value, filter.value) match {

          case (t: AnalyticModelFilterType, Some(value)) =>
            val result =
              (t.modelFilter _).asInstanceOf[T#Content => AnalyticOperation => Boolean](value)(model) // scalafix:ok

            FilterApplier.incrementFalse(t, "analytic_model").unless(result).as(result)

          case _ =>
            ZIO.succeed(true)
        }
    }

  implicit def modelFilterInstance[T <: ModelFilterType](implicit
    w: Witness.Aux[T]
  ): ModelFilterApplier[Filter[T], Operation] =
    new ModelFilterApplier[Filter[T], Operation] {
      override def apply(filter: Filter[T], model: Operation): UIO[Boolean] =
        (w.value, filter.value) match {

          case (t: ModelFilterType, Some(value)) =>
            val result =
              (t.modelFilter _).asInstanceOf[T#Content => Operation => Boolean](value)(model) // scalafix:ok

            FilterApplier.incrementFalse(t, "model").unless(result).as(result)

          case _ =>
            ZIO.succeed(true)
        }
    }

  implicit val analyticModelHNilInstance: ModelFilterApplier[HNil, AnalyticOperation] =
    new ModelFilterApplier[HNil, AnalyticOperation] {
      override def apply(filter: HNil, model: AnalyticOperation): UIO[Boolean] =
        FilterApplier.incrementTrue("analytic_model").as(true)
    }

  implicit val modelHNilInstance: ModelFilterApplier[HNil, Operation] =
    new ModelFilterApplier[HNil, Operation] {
      override def apply(filter: HNil, model: Operation): UIO[Boolean] =
        FilterApplier.incrementTrue("model").as(true)
    }

  implicit def modelHListInstance[H, L <: HList, O <: AnalyticOperation](implicit
    headApplier: ModelFilterApplier[H, O],
    tailApplier: ModelFilterApplier[L, O]
  ): ModelFilterApplier[H :: L, O] = new ModelFilterApplier[H :: L, O] {
    override def apply(filter: H :: L, model: O): UIO[Boolean] =
      filter match {
        case head :: tail => ifZIO(headApplier(head, model))(tailApplier(tail, model), ZIO.succeed(false))
      }
  }
}

object RichFilterApplier {
  implicit def analyticRichFilterInstance[T <: AnalyticFilterType](implicit
    w: Witness.Aux[T]
  ): RichFilterApplier[Filter[T], AnalyticOperation, AnalyticRichOperation] =
    new RichFilterApplier[Filter[T], AnalyticOperation, AnalyticRichOperation] {
      override def apply(filter: Filter[T], model: AnalyticOperation): UIO[Boolean] =
        (w.value, filter.value) match {

          case (t: PartiallyAnalyticModelFilterType, Some(value)) =>
            val result =
              (t.modelFilter _).asInstanceOf[T#Content => AnalyticOperation => Boolean](value)(model) // scalafix:ok

            FilterApplier.incrementFalse(t, "analytic_model").unless(result).as(result)

          case _ =>
            ZIO.succeed(true)
        }

      override def apply(filter: Filter[T], operation: AnalyticRichOperation): UIO[Boolean] =
        (w.value, filter.value) match {

          case (t: AnalyticFilterType, Some(value)) =>
            val result =
              (t.filter _).asInstanceOf[T#Content => AnalyticRichOperation => Boolean](value)(operation) // scalafix:ok

            FilterApplier.incrementFalse(t, "analytic_rich").unless(result).as(result)

          case _ =>
            ZIO.succeed(true)
        }
    }

  implicit def richFilterInstance[T <: FilterType](implicit
    w: Witness.Aux[T]
  ): RichFilterApplier[Filter[T], Operation, RichOperation] =
    new RichFilterApplier[Filter[T], Operation, RichOperation] {
      override def apply(filter: Filter[T], model: Operation): UIO[Boolean] =
        (w.value, filter.value) match {

          case (t: PartiallyModelFilterType, Some(value)) =>
            val result =
              (t.modelFilter _).asInstanceOf[T#Content => Operation => Boolean](value)(model) // scalafix:ok

            FilterApplier.incrementFalse(t, "model").unless(result).as(result)

          case _ =>
            ZIO.succeed(true)
        }

      override def apply(filter: Filter[T], operation: RichOperation): UIO[Boolean] =
        (w.value, filter.value) match {

          case (t: FilterType, Some(value)) =>
            val result =
              (t.filter _).asInstanceOf[T#Content => RichOperation => Boolean](value)(operation) // scalafix:ok

            FilterApplier.incrementFalse(t, "rich").unless(result).as(result)

          case _ =>
            ZIO.succeed(true)
        }
    }

  implicit val analyticRichHNilInstance: RichFilterApplier[HNil, AnalyticOperation, AnalyticRichOperation] =
    new RichFilterApplier[HNil, AnalyticOperation, AnalyticRichOperation] {
      override def apply(filter: HNil, operation: AnalyticOperation): UIO[Boolean] =
        FilterApplier.incrementTrue("analytic_model").as(true)

      override def apply(filter: HNil, operation: AnalyticRichOperation): UIO[Boolean] =
        FilterApplier.incrementTrue("analytic_rich").as(true)
    }

  implicit val richHNilInstance: RichFilterApplier[HNil, Operation, RichOperation] =
    new RichFilterApplier[HNil, Operation, RichOperation] {
      override def apply(filter: HNil, operation: Operation): UIO[Boolean] =
        FilterApplier.incrementTrue("model").as(true)

      override def apply(filter: HNil, operation: RichOperation): UIO[Boolean] =
        FilterApplier.incrementTrue("rich").as(true)
    }

  implicit def richHListInstance[H, L <: HList, O <: AnalyticOperation, RO <: AnalyticRichOperation](implicit
    headApplier: RichFilterApplier[H, O, RO],
    tailApplier: RichFilterApplier[L, O, RO]
  ): RichFilterApplier[H :: L, O, RO] = new RichFilterApplier[H :: L, O, RO] {

    override def apply(filter: H :: L, model: O): UIO[Boolean] =
      filter match {
        case head :: tail => ifZIO(headApplier(head, model))(tailApplier(tail, model), ZIO.succeed(false))
      }

    override def apply(filter: H :: L, rich: RO): UIO[Boolean] =
      filter match {
        case head :: tail => ifZIO(headApplier(head, rich))(tailApplier(tail, rich), ZIO.succeed(false))
      }
  }
}
