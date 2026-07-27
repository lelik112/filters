package net.cheltsov

import zio.test.{ZIOSpecDefault, assertTrue}

import net.cheltsov.TestFixtures._
import net.cheltsov.filters.FilterType.{AccountTypeFilter, ComplexFilter}
import net.cheltsov.filters.{Filter, ModelFiltersProduct, RichFiltersProduct}
import net.cheltsov.model.{AccountType, Operation, RichOperation}

object FiltersProductSpec extends ZIOSpecDefault {

  final case class ModelFilterGroup(
    first:  Filter[AccountTypeFilter.type],
    second: Filter[AccountTypeFilter.type]
  )

  final case class RichFilterGroup(
    accountType: Filter[AccountTypeFilter.type],
    complex:     Filter[ComplexFilter.type]
  )

  override def spec =
    suite("Filter products")(
      suite("model products")(
        test("a single filter is converted and applied") {
          val product = implicitly[ModelFiltersProduct[Filter[AccountTypeFilter.type], Operation]]
          val repr    = product.toRepr(AccountTypeFilter.of(AccountType.Prime))

          for {
            accountResult <- product.accountApplier(repr, PrimeAccount)
            modelResult   <- product.modelApplier(repr, PrimeOperation)
          } yield assertTrue(accountResult, modelResult)
        },
        test("a tuple is combined and all predicates must pass") {
          type Filters = (Filter[AccountTypeFilter.type], Filter[AccountTypeFilter.type])

          val product = implicitly[ModelFiltersProduct[Filters, Operation]]
          val filters = AccountTypeFilter.of(AccountType.Prime) -> AccountTypeFilter.of(AccountType.Deposit)
          val repr    = product.toRepr(filters)

          for {
            accountResult <- product.accountApplier(repr, PrimeAccount)
            modelResult   <- product.modelApplier(repr, PrimeOperation)
          } yield assertTrue(!accountResult, !modelResult)
        },
        test("a case class is derived recursively") {
          val product = implicitly[ModelFiltersProduct[ModelFilterGroup, Operation]]
          val filters =
            ModelFilterGroup(first = AccountTypeFilter.of(AccountType.Prime), second = AccountTypeFilter.empty)
          val repr    = product.toRepr(filters)

          for {
            accountResult <- product.accountApplier(repr, PrimeAccount)
            modelResult   <- product.modelApplier(repr, PrimeOperation)
          } yield assertTrue(accountResult, modelResult)
        }
      ),
      suite("rich products")(
        test("a complex filter is applied at the correct stages") {
          val product = implicitly[RichFiltersProduct[Filter[ComplexFilter.type], Operation, RichOperation]]
          val repr    = product.toRepr(ComplexFilter.of(PrimeBrandId -> PrimeCategoryId))

          for {
            accountResult <- product.accountApplier(repr, PrimeAccount)
            modelResult   <- product.modelApplier(repr, PrimeOperation)
            richResult    <- product.richApplier(repr, PrimeRichOperation)
          } yield assertTrue(accountResult, modelResult, richResult)
        },
        test("a tuple can combine account, model and rich-stage filtering") {
          type Filters = (Filter[AccountTypeFilter.type], Filter[ComplexFilter.type])

          val product = implicitly[RichFiltersProduct[Filters, Operation, RichOperation]]
          val filters = AccountTypeFilter.of(AccountType.Prime) -> ComplexFilter.of(PrimeBrandId -> PrimeCategoryId)
          val repr    = product.toRepr(filters)

          for {
            accountResult <- product.accountApplier(repr, PrimeAccount)
            modelResult   <- product.modelApplier(repr, PrimeOperation)
            richResult    <- product.richApplier(repr, PrimeRichOperation)
            wrongCategory <- product.richApplier(repr, PrimeRichOperationWithOtherCategory)
          } yield assertTrue(accountResult, modelResult, richResult, !wrongCategory)
        },
        test("a rich filter case class is derived recursively") {
          val product = implicitly[RichFiltersProduct[RichFilterGroup, Operation, RichOperation]]
          val filters = RichFilterGroup(
            accountType = AccountTypeFilter.of(AccountType.Prime),
            complex = ComplexFilter.of(PrimeBrandId -> PrimeCategoryId)
          )
          val repr    = product.toRepr(filters)

          for {
            accountResult <- product.accountApplier(repr, PrimeAccount)
            modelResult   <- product.modelApplier(repr, PrimeOperation)
            richResult    <- product.richApplier(repr, PrimeRichOperation)
          } yield assertTrue(accountResult, modelResult, richResult)
        }
      )
    )
}
