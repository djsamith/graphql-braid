package com.atlassian.braid.document;

import com.atlassian.braid.document.SelectionOperation.OperationResult;
import com.atlassian.braid.mapper.Mapper;
import com.atlassian.braid.mapper.MapperOperation;
import com.atlassian.braid.mapper.MapperOperations;
import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.SelectionSet;

import java.util.function.BiFunction;

import static com.atlassian.braid.document.Fields.cloneFieldWithNewSelectionSet;
import static com.atlassian.braid.document.Fields.getFieldAliasOrName;
import static com.atlassian.braid.document.SelectionOperation.result;
import static com.atlassian.braid.mapper.Mappers.mapper;
import static java.util.Objects.requireNonNull;

/**
 * <p>This is an intermediary <strong>internal</strong> <em>mutable</em> class for {@link SelectionSet selection set}
 * mapping results
 */
final class SelectionSetMappingResult {
    private final SelectionSet selectionSet;
    private final MapperOperation resultMapper;

    SelectionSetMappingResult(SelectionSet selectionSet, MapperOperation resultMapper) {
        this.selectionSet = requireNonNull(selectionSet);
        this.resultMapper = requireNonNull(resultMapper);
    }

    public SelectionSet getSelectionSet() {
        return selectionSet;
    }

    OperationResult toOperationResult(Field field, MappingContext mappingContext) {
        return result(
                cloneFieldWithNewSelectionSet(field, selectionSet),
                getMapperOperation(mappingContext).apply(getFieldAliasOrName(field), mapper(resultMapper)));
    }

    OperationResult toOperationResult(InlineFragment inlineFragment) {
        return result(
                new InlineFragment(inlineFragment.getTypeCondition(), inlineFragment.getDirectives(), selectionSet),
                resultMapper);
    }

    OperationResult toOperationResult(FragmentSpread fragmentSpread) {
        return result(fragmentSpread, resultMapper);
    }

    private static BiFunction<String, Mapper, MapperOperation> getMapperOperation(MappingContext mappingContext) {
        return mappingContext.inList() ? MapperOperations::copyList : MapperOperations::map;
    }
}
