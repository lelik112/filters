package net.cheltsov.model

/**
 * Model of operation. We get it from different services (each for every master systems depends on AccountType).
 * Some services do a lot of work to create their own SpecificFields. It is the reason why I have added AnalyticOperation
 * which has light version of fields - AnalyticSpecificFields
 */
final case class Operation(id: OperationId, amount: BigDecimal, accountId: AccountId, specificFields: SpecificFields)
    extends AnalyticOperation
