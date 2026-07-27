package net.cheltsov

import zio.test.{ZIOSpecDefault, assertTrue, suite, test}

import net.cheltsov.TestFixtures._
import net.cheltsov.filters.Filter
import net.cheltsov.filters.FilterType.{AccountTypeFilter, ComplexFilter}
import net.cheltsov.model.AccountType

object FilterSpec extends ZIOSpecDefault {

  override def spec =
    suite("Filter and filter definitions")(
      test("Filter.empty has no value") {
        val filter = Filter.empty[AccountTypeFilter.type]

        assertTrue(filter.isEmpty, !filter.nonEmpty, filter.value.isEmpty)
      },
      test("FilterType.of stores typed content") {
        val filter = AccountTypeFilter.of(AccountType.Prime)

        assertTrue(filter.nonEmpty, filter.value.contains(AccountType.Prime))
      },
      test("AccountTypeFilter filters accounts") {
        val predicate = AccountTypeFilter.accountFilter(AccountType.Prime)

        assertTrue(predicate(PrimeAccount), !predicate(DepositAccount))
      },
      test("AccountTypeFilter filters analytical models") {
        val predicate = AccountTypeFilter.modelFilter(AccountType.Prime)

        assertTrue(predicate(PrimeOperation), !predicate(DepositOperation))
      },
      test("ComplexFilter performs cheap partial filtering on Operation") {
        val filter = ComplexFilter.modelFilter(PrimeBrandId -> OtherCategoryId)

        assertTrue(filter(PrimeOperation), !filter(DepositOperation))
      },
      test("ComplexFilter performs the full check on RichOperation") {
        val filter = ComplexFilter.filter(PrimeBrandId -> PrimeCategoryId)

        assertTrue(filter(PrimeRichOperation), !filter(PrimeRichOperationWithOtherCategory))
      }
    )
}
