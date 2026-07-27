package net.cheltsov

import net.cheltsov.model._

private[cheltsov] object TestFixtures {
  val PrimeAccountId: AccountId       = AccountId("account-prime")
  val DepositAccountId: AccountId     = AccountId("account-deposit")
  val PrimeOperationId: OperationId   = OperationId("operation-prime")
  val DepositOperationId: OperationId = OperationId("operation-deposit")
  val PrimeBrandId: BrandId           = BrandId("brand-prime")
  val DepositBrandId: BrandId         = BrandId("brand-deposit")
  val PrimeCategoryId: CategoryId     = CategoryId("category-prime")
  val OtherCategoryId: CategoryId     = CategoryId("category-other")

  val PrimeAccount: Account =
    Account(PrimeAccountId, "Prime account", AccountType.Prime)

  val DepositAccount: Account =
    Account(DepositAccountId, "Deposit account", AccountType.Deposit)

  val PrimeOperation: Operation =
    Operation(
      id = PrimeOperationId,
      amount = BigDecimal(100),
      accountId = PrimeAccountId,
      specificFields = SpecificFields.PrimeSpecific(brandId = PrimeBrandId)
    )

  val DepositOperation: Operation =
    Operation(
      id = DepositOperationId,
      amount = BigDecimal(200),
      accountId = DepositAccountId,
      specificFields = SpecificFields.DepositSpecific(brandId = DepositBrandId)
    )

  val PrimeRichOperation: RichOperation =
    RichOperation(PrimeOperation, PrimeCategoryId)

  val PrimeRichOperationWithOtherCategory: RichOperation =
    RichOperation(PrimeOperation, OtherCategoryId)
}
