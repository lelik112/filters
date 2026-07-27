package net.cheltsov

import zio.test.{ZIOSpecDefault, assertTrue}

import net.cheltsov.TestFixtures._
import net.cheltsov.filters.FilterType.{AccountTypeFilter, ComplexFilter}
import net.cheltsov.filters.{Filter, FilterApplier, ModelFilterApplier, RichFilterApplier}
import net.cheltsov.model.{AccountType, AnalyticOperation, AnalyticRichOperation, Operation, RichOperation}

import shapeless.{::, HNil}

object FilterApplierSpec extends ZIOSpecDefault {

  override def spec =
    suite("Filter appliers")(
      suite("account stage")(
        test("a matching account filter returns true") {
          val applier = implicitly[FilterApplier[Filter[AccountTypeFilter.type]]]

          for {
            result <- applier(AccountTypeFilter.of(AccountType.Prime), PrimeAccount)
          } yield assertTrue(result)
        },
        test("a non-matching account filter returns false") {
          val applier = implicitly[FilterApplier[Filter[AccountTypeFilter.type]]]

          for {
            result <- applier(AccountTypeFilter.of(AccountType.Deposit), PrimeAccount)
          } yield assertTrue(!result)
        },
        test("an empty account filter is ignored") {
          val applier = implicitly[FilterApplier[Filter[AccountTypeFilter.type]]]

          for {
            result <- applier(AccountTypeFilter.empty, PrimeAccount)
          } yield assertTrue(result)
        },
        test("an HList combines account filters with AND") {
          val applier = implicitly[
            FilterApplier[Filter[AccountTypeFilter.type] :: Filter[AccountTypeFilter.type] :: HNil]
          ]
          val filters =
            AccountTypeFilter.of(AccountType.Prime) ::
              AccountTypeFilter.of(AccountType.Deposit) ::
              HNil

          for {
            result <- applier(filters, PrimeAccount)
          } yield assertTrue(!result)
        },
        test("HNil accepts every account") {
          val applier = implicitly[FilterApplier[HNil]]

          for {
            result <- applier(HNil, PrimeAccount)
          } yield assertTrue(result)
        }
      ),
      suite("model stage")(
        test("the operation applier evaluates a model filter") {
          val applier = implicitly[ModelFilterApplier[Filter[AccountTypeFilter.type], Operation]]

          for {
            accepted <- applier(AccountTypeFilter.of(AccountType.Prime), PrimeOperation)
            rejected <- applier(AccountTypeFilter.of(AccountType.Deposit), PrimeOperation)
          } yield assertTrue(accepted, !rejected)
        },
        test("the analytical applier evaluates an analytical model filter") {
          val applier = implicitly[ModelFilterApplier[Filter[AccountTypeFilter.type], AnalyticOperation]]

          for {
            accepted <- applier(AccountTypeFilter.of(AccountType.Prime), PrimeOperation)
            rejected <- applier(AccountTypeFilter.of(AccountType.Deposit), PrimeOperation)
          } yield assertTrue(accepted, !rejected)
        },
        test("an empty model filter is ignored") {
          val applier = implicitly[ModelFilterApplier[Filter[AccountTypeFilter.type], Operation]]

          for {
            result <- applier(AccountTypeFilter.empty, PrimeOperation)
          } yield assertTrue(result)
        },
        test("HNil accepts every model") {
          val applier = implicitly[ModelFilterApplier[HNil, Operation]]

          for {
            result <- applier(HNil, PrimeOperation)
          } yield assertTrue(result)
        }
      ),
      suite("rich stage")(
        test("ComplexFilter applies only its cheap part at the model stage") {
          val applier = implicitly[RichFilterApplier[Filter[ComplexFilter.type], Operation, RichOperation]]
          val filter  = ComplexFilter.of(PrimeBrandId -> OtherCategoryId)

          for {
            result <- applier(filter, PrimeOperation)
          } yield assertTrue(result)
        },
        test("ComplexFilter applies its full predicate at the rich stage") {
          val applier = implicitly[RichFilterApplier[Filter[ComplexFilter.type], Operation, RichOperation]]
          val filter  = ComplexFilter.of(PrimeBrandId -> PrimeCategoryId)

          for {
            accepted <- applier(filter, PrimeRichOperation)
            rejected <- applier(filter, PrimeRichOperationWithOtherCategory)
          } yield assertTrue(accepted, !rejected)
        },
        test("an analytical rich applier delegates AccountTypeFilter to its model") {
          val applier = implicitly[
            RichFilterApplier[Filter[AccountTypeFilter.type], AnalyticOperation, AnalyticRichOperation]
          ]

          for {
            accepted <- applier(AccountTypeFilter.of(AccountType.Prime), PrimeRichOperation)
            rejected <- applier(AccountTypeFilter.of(AccountType.Deposit), PrimeRichOperation)
          } yield assertTrue(accepted, !rejected)
        },
        test("an empty rich filter is ignored at both stages") {
          val applier = implicitly[RichFilterApplier[Filter[ComplexFilter.type], Operation, RichOperation]]

          for {
            modelResult <- applier(ComplexFilter.empty, PrimeOperation)
            richResult  <- applier(ComplexFilter.empty, PrimeRichOperation)
          } yield assertTrue(modelResult, richResult)
        },
        test("HNil accepts every rich model") {
          val applier = implicitly[RichFilterApplier[HNil, Operation, RichOperation]]

          for {
            modelResult <- applier(HNil, PrimeOperation)
            richResult  <- applier(HNil, PrimeRichOperation)
          } yield assertTrue(modelResult, richResult)
        }
      )
    )
}
