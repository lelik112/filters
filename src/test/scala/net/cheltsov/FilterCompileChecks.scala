package net.cheltsov

import net.cheltsov.Examples.{AnalyticFilters, NonApiFilters, OperationFilters}
import net.cheltsov.filters.FilterType._
import net.cheltsov.filters.{Filter, FilterInput, ModelFiltersProduct, RichFiltersProduct}
import net.cheltsov.model.{AnalyticOperation, AnalyticRichOperation, Operation, RichOperation}

import shapeless.test.illTyped
import shapeless.{::, HNil}

/**
 * Compile-time contract tests.
 *
 * Positive examples below must compile. Each illTyped block must fail type
 * checking; if an invalid combination becomes accepted, Test / compile fails.
 */
object FilterCompileChecks {

  private final case class ModelOnlyFilters(
    accountType: Filter[AccountTypeFilter.type],
    brandIds:    Filter[BrandIds.type]
  )

  private final case class AnalyticModelOnlyFilters(
    accountType: Filter[AccountTypeFilter.type],
    accountIds:  Filter[AccountIds.type]
  )

  private val service: OperationsService = OperationsService()

  // ---------------------------------------------------------------------------
  // Positive service API examples
  // ---------------------------------------------------------------------------

  private def validServiceCalls(): Unit = {
    service.operations(AccountTypeFilter.empty)
    service.operations(AccountTypeFilter.empty         -> BrandIds.empty)
    service.analyticOperations(AccountTypeFilter.empty)
    service.analyticOperations(AccountTypeFilter.empty -> AccountIds.empty)

    service.richOperations(CategoryIds.empty)
    service.richOperations(ComplexFilter.empty)
    service.richOperations(AccountTypeFilter.empty -> CategoryIds.empty)
    service.richOperations(OperationFilters.Empty)
    service.richOperations(OperationFilters.Empty  -> (AccountTypeFilter.empty -> CategoryIds.empty))

    service.analyticRichOperations(AccountIds.empty)
    service.analyticRichOperations(AccountTypeFilter.empty -> OperationIds.empty)
    service.analyticRichOperations(AnalyticFilters.Empty)
  }

  // ---------------------------------------------------------------------------
  // Positive type-class derivation examples
  // ---------------------------------------------------------------------------

  implicitly[ModelFiltersProduct[Filter[AccountTypeFilter.type], Operation]]
  implicitly[ModelFiltersProduct[Filter[BrandIds.type], Operation]]
  implicitly[ModelFiltersProduct[(Filter[AccountTypeFilter.type], Filter[BrandIds.type]), Operation]]
  implicitly[ModelFiltersProduct[ModelOnlyFilters, Operation]]

  implicitly[ModelFiltersProduct[Filter[AccountTypeFilter.type], AnalyticOperation]]
  implicitly[ModelFiltersProduct[AnalyticModelOnlyFilters, AnalyticOperation]]
  implicitly[ModelFiltersProduct[Filter[AccountIds.type], AnalyticOperation]]
  implicitly[ModelFiltersProduct[(Filter[AccountTypeFilter.type], Filter[AccountIds.type]), AnalyticOperation]]

  implicitly[RichFiltersProduct[Filter[CategoryIds.type], Operation, RichOperation]]
  implicitly[RichFiltersProduct[Filter[ComplexFilter.type], Operation, RichOperation]]
  implicitly[RichFiltersProduct[(Filter[AccountTypeFilter.type], Filter[CategoryIds.type]), Operation, RichOperation]]
  implicitly[RichFiltersProduct[OperationFilters, Operation, RichOperation]]

  implicitly[RichFiltersProduct[Filter[AccountIds.type], AnalyticOperation, AnalyticRichOperation]]
  implicitly[
    RichFiltersProduct[
      (Filter[AccountTypeFilter.type], Filter[OperationIds.type]),
      AnalyticOperation,
      AnalyticRichOperation
    ]
  ]
  implicitly[RichFiltersProduct[AnalyticFilters, AnalyticOperation, AnalyticRichOperation]]

  implicitly[FilterInput[Filter[AccountIds.type]]]
  implicitly[FilterInput[Filter[OperationIds.type]]]
  implicitly[FilterInput[Filter[CategoryIds.type]]]
  implicitly[FilterInput[Filter[BrandIds.type]]]
  implicitly[
    FilterInput[
      Filter[AccountIds.type] ::
        Filter[OperationIds.type] ::
        Filter[CategoryIds.type] ::
        Filter[BrandIds.type] ::
        HNil
    ]
  ]

  // ---------------------------------------------------------------------------
  // Invalid model-stage combinations
  // ---------------------------------------------------------------------------

  illTyped("""
    service.operations(CategoryIds.empty)
  """)

  illTyped("""
    service.operations(ComplexFilter.empty)
  """)

  illTyped("""
    service.analyticOperations(BrandIds.empty)
  """)

  illTyped("""
    service.analyticOperations(CategoryIds.empty)
  """)

  illTyped("""
    implicitly[ModelFiltersProduct[Filter[CategoryIds.type], Operation]]
  """)

  illTyped("""
    implicitly[ModelFiltersProduct[Filter[ComplexFilter.type], Operation]]
  """)

  illTyped("""
    implicitly[ModelFiltersProduct[Filter[BrandIds.type], AnalyticOperation]]
  """)

  // ---------------------------------------------------------------------------
  // Invalid rich-stage combinations
  // ---------------------------------------------------------------------------

  illTyped("""
    service.analyticRichOperations(BrandIds.empty)
  """)

  illTyped("""
    service.analyticRichOperations(CategoryIds.empty)
  """)

  illTyped("""
    service.analyticRichOperations(ComplexFilter.empty)
  """)

  illTyped("""
    implicitly[RichFiltersProduct[Filter[BrandIds.type], AnalyticOperation, AnalyticRichOperation]]
  """)

  illTyped("""
    implicitly[RichFiltersProduct[Filter[CategoryIds.type], AnalyticOperation, AnalyticRichOperation]]
  """)

  illTyped("""
    implicitly[RichFiltersProduct[Filter[ComplexFilter.type], AnalyticOperation, AnalyticRichOperation]]
  """)

  // ---------------------------------------------------------------------------
  // Invalid Tapir input derivation
  // ---------------------------------------------------------------------------

  illTyped("""
    implicitly[FilterInput[Filter[AccountTypeFilter.type]]]
  """)

  illTyped("""
    implicitly[FilterInput[Filter[ComplexFilter.type]]]
  """)

  illTyped("""
    FilterInput.input(shapeless.Generic[NonApiFilters].to(NonApiFilters.Empty))
  """)
}
