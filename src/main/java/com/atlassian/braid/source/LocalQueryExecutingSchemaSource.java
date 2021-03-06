package com.atlassian.braid.source;

import com.atlassian.braid.Link;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import com.atlassian.braid.document.DocumentMapper;
import com.atlassian.braid.document.DocumentMapperFactory;
import com.atlassian.braid.document.DocumentMappers;
import graphql.ExecutionInput;
import graphql.execution.DataFetcherResult;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.atlassian.braid.java.util.BraidObjects.cast;
import static com.atlassian.braid.source.SchemaUtils.loadSchema;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Local schema source
 */
@SuppressWarnings("WeakerAccess")
public final class LocalQueryExecutingSchemaSource extends ForwardingSchemaSource implements QueryExecutorSchemaSource {
    private final BaseQueryExecutorSchemaSource delegate;
    private final Function<ExecutionInput, Object> queryExecutor;

    public LocalQueryExecutingSchemaSource(SchemaNamespace namespace,
                                           Supplier<Reader> schemaProvider,
                                           Function<ExecutionInput, Object> queryExecutor) {
        this(namespace, schemaProvider, emptyList(), DocumentMappers.identity(), queryExecutor);
    }

    public LocalQueryExecutingSchemaSource(SchemaNamespace namespace,
                                           Supplier<Reader> schemaProvider,
                                           List<Link> links,
                                           DocumentMapperFactory documentMapper,
                                           Function<ExecutionInput, Object> queryExecutor) {
        this(namespace, loadSchema(schemaProvider), links, documentMapper, queryExecutor);
    }

    public LocalQueryExecutingSchemaSource(SchemaNamespace namespace,
                                           TypeDefinitionRegistry schema,
                                           List<Link> links,
                                           DocumentMapperFactory documentMapper,
                                           Function<ExecutionInput, Object> queryExecutor) {
        this.queryExecutor = requireNonNull(queryExecutor);
        this.delegate = new BaseQueryExecutorSchemaSource<>(namespace,
                schema,
                links,
                documentMapper,
                this::query);

    }

    @Override
    protected SchemaSource getDelegate() {
        return delegate;
    }

    @Override
    public DocumentMapper getDocumentMapper() {
        return delegate.getDocumentMapper();
    }

    private <C> CompletableFuture<DataFetcherResult<Map<String, Object>>> query(ExecutionInput executionInput, C context) {
        final Object result = queryExecutor.apply(transformExecutionInput(executionInput, context));
        if (result instanceof DataFetcherResult) {
            return completedFuture((cast(result)));
        } else if (result instanceof Map) {
            return completedFuture(new DataFetcherResult<>(cast(result), emptyList()));
        } else {
            CompletableFuture<DataFetcherResult<Map<String, Object>>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Unexpected result type: " + nullSafeGetClass(result)));
            return future;
        }
    }

    private <C> ExecutionInput transformExecutionInput(ExecutionInput executionInput, C context) {
        return executionInput.transform(builder -> builder.context(context));
    }

    private static Class<?> nullSafeGetClass(Object result) {
        return Optional.ofNullable(result).map(Object::getClass).orElse(null);
    }
}
