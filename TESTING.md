# Test suite

The test suite has two layers.

## Runtime tests

- `FilterSpec` checks the filter value wrapper and the pure predicates.
- `FilterApplierSpec` checks account, model, analytical, and rich-stage appliers, including empty filters and HLists.
- `FiltersProductSpec` checks derivation for individual filters, tuples, and case classes.

## Compile-time contract tests

`FilterCompileChecks` verifies that supported filter combinations compile and unsupported combinations do not compile. Negative checks use `shapeless.test.illTyped`.

`shapeless.test.illTyped` is provided by the Shapeless dependency already used by the project.
