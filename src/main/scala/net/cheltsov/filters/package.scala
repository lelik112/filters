package net.cheltsov

import zio.UIO

import net.cheltsov.model.{Account, AnalyticOperation, AnalyticRichOperation}

package object filters {
  implicit final class AccountFilteringSupportOps[F](private val f: F)(implicit product: FiltersProduct[F]) {
    def accountFilter: Account => UIO[Boolean] =
      product.accountApplier.apply(product.toRepr(f), _)
  }

  implicit final class ModelFilteringSupportOps[F, O <: AnalyticOperation](
    private val f: F
  )(implicit product: ModelFiltersProduct[F, O]) {
    def modelFilter: O => UIO[Boolean] =
      product.modelApplier.apply(product.toRepr(f), _)

  }

  implicit final class RichFilteringSupportOps[F, O <: AnalyticOperation, RO <: AnalyticRichOperation](
    private val f: F
  )(implicit product: RichFiltersProduct[F, O, RO]) {
    def richFilter: RO => UIO[Boolean] =
      product.richApplier.apply(product.toRepr(f), _)
  }

}
