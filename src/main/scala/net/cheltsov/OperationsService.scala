package net.cheltsov

import zio.UIO
import zio.prelude.ForEachOps

import net.cheltsov.filters._
import net.cheltsov.model.{Account, AnalyticOperation, AnalyticRichOperation, Operation, RichOperation}

trait OperationsService {
  def analyticOperations[FP](filters: FP)(implicit
    mfp: ModelFiltersProduct[FP, AnalyticOperation]
  ): UIO[List[AnalyticOperation]]

  def analyticRichOperations[FP](filters: FP)(implicit
    mfp: RichFiltersProduct[FP, AnalyticOperation, AnalyticRichOperation]
  ): UIO[List[AnalyticRichOperation]]

  def operations[FP](filters: FP)(implicit
    mfp: ModelFiltersProduct[FP, Operation]
  ): UIO[List[Operation]]

  def richOperations[FP](filters: FP)(implicit
    mfp: RichFiltersProduct[FP, Operation, RichOperation]
  ): UIO[List[RichOperation]]
}

object OperationsService {
  def apply(): OperationsService = new OperationsService {

    private val accounts: UIO[List[Account]] = ???
    private def analyticOperations(account:              Account): UIO[List[AnalyticOperation]]         = ???
    private def analyticRichOperation(analyticOperation: AnalyticOperation): UIO[AnalyticRichOperation] = ???
    private def operations(account:                      Account): UIO[List[Operation]]                 = ???
    private def richOperation(operation:                 Operation): UIO[RichOperation]                 = ???

    private def filteredAccounts[FP: FiltersProduct](filters: FP): UIO[List[Account]] =
      accounts.flatMap(_.filterM(filters.accountFilter))

    override def analyticOperations[FP](filters: FP)(implicit
      mfp: ModelFiltersProduct[FP, AnalyticOperation]
    ): UIO[List[AnalyticOperation]] =
      filteredAccounts(filters)                        // First filtration of List[Account]
        .flatMap(_.forEachFlatten(analyticOperations)) // Getting List[AnalyticOperation] for each account
        .flatMap(_.filterM(filters.modelFilter)) // Second filtration of AnalyticOperation

    override def analyticRichOperations[FP](filters: FP)(implicit
      mfp: RichFiltersProduct[FP, AnalyticOperation, AnalyticRichOperation]
    ): UIO[List[AnalyticRichOperation]] =
      filteredAccounts(filters)                        // First filtration of List[Account]
        .flatMap(_.forEachFlatten(analyticOperations)) // Getting List[AnalyticOperation] for each account
        .flatMap(_.filterM(filters.modelFilter))       // Second filtration of List[AnalyticOperation]
        .flatMap(_.forEach(analyticRichOperation))     // Making AnalyticRichOperation from AnalyticOperation
        .flatMap(_.filterM(filters.richFilter)) // Third filtration of List[AnalyticRichOperation]

    override def operations[FP](filters: FP)(implicit
      mfp: ModelFiltersProduct[FP, Operation]
    ): UIO[List[Operation]] =
      filteredAccounts(filters)                // First filtration of List[Account]
        .flatMap(_.forEachFlatten(operations)) // Getting List[Operation] for each account
        .flatMap(_.filterM(filters.modelFilter)) // Second filtration of Operation

    override def richOperations[FP](filters: FP)(implicit
      mfp: RichFiltersProduct[FP, Operation, RichOperation]
    ): UIO[List[RichOperation]] =
      filteredAccounts(filters)                  // First filtration of List[Account]
        .flatMap(_.forEachFlatten(operations))   // Getting List[Operation] for each account
        .flatMap(_.filterM(filters.modelFilter)) // Second filtration of Operation
        .flatMap(_.forEach(richOperation))       // Making RichOperation from Operation
        .flatMap(_.filterM(filters.richFilter)) // Third filtration of List[RichOperation]
  }
}
