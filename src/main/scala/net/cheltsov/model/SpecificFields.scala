package net.cheltsov.model

sealed trait AnalyticSpecificFields {
  def accountType: AccountType
}

sealed trait SpecificFields extends AnalyticSpecificFields {
  def brandId: BrandId
}

object SpecificFields {
  final case class PrimeSpecific(accountType: AccountType.Prime.type = AccountType.Prime, brandId: BrandId)
      extends SpecificFields

  final case class DepositSpecific(accountType: AccountType.Deposit.type = AccountType.Deposit, brandId: BrandId)
      extends SpecificFields

}
