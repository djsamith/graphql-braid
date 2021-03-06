package com.atlassian.braid.source;

import com.atlassian.braid.Link;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import com.atlassian.braid.document.DocumentMapperFactory;
import com.atlassian.braid.document.DocumentMappers;
import com.atlassian.braid.java.util.BraidObjects;
import graphql.ExecutionInput;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.atlassian.braid.source.SchemaUtils.loadPublicSchema;
import static com.atlassian.braid.source.SchemaUtils.loadSchema;
import static java.util.Objects.requireNonNull;

/**
 * Data source for an external graphql service.  Loads the schema on construction.
 */
@SuppressWarnings("WeakerAccess")
public final class GraphQLRemoteSchemaSource<C> extends ForwardingSchemaSource implements QueryExecutorSchemaSource {

    private final BaseQueryExecutorSchemaSource<C> delegate;
    private final GraphQLRemoteRetriever<C> graphQLRemoteRetriever;


    public GraphQLRemoteSchemaSource(SchemaNamespace namespace,
                                     Supplier<Reader> schemaProvider,
                                     GraphQLRemoteRetriever<C> graphQLRemoteRetriever,
                                     List<Link> links,
                                     String... topLevelFields) {
        this(namespace, schemaProvider, graphQLRemoteRetriever, links, DocumentMappers.identity(), topLevelFields);
    }

    public GraphQLRemoteSchemaSource(SchemaNamespace namespace,
                                     Supplier<Reader> schemaProvider,
                                     GraphQLRemoteRetriever<C> graphQLRemoteRetriever,
                                     List<Link> links,
                                     DocumentMapperFactory documentMapperFactory,
                                     String... topLevelFields) {
        this(namespace,
                loadPublicSchema(schemaProvider, topLevelFields),
                loadSchema(schemaProvider), graphQLRemoteRetriever, links, documentMapperFactory);
    }

    public GraphQLRemoteSchemaSource(SchemaNamespace namespace,
                                     TypeDefinitionRegistry publicSchema,
                                     TypeDefinitionRegistry privateSchema,
                                     GraphQLRemoteRetriever<C> graphQLRemoteRetriever,
                                     List<Link> links,
                                     DocumentMapperFactory documentMapperFactory) {
        this.graphQLRemoteRetriever = requireNonNull(graphQLRemoteRetriever);
        this.delegate = new BaseQueryExecutorSchemaSource<>(namespace,
                publicSchema,
                privateSchema,
                links,
                documentMapperFactory,
                this::query);

    }


    @Override
    protected SchemaSource getDelegate() {
        return delegate;
    }

    // visible for testing
    public CompletableFuture<DataFetcherResult<Map<String, Object>>> query(ExecutionInput query, C context) {
        return graphQLRemoteRetriever.queryGraphQL(query, context).thenApply(response -> {

            Map<String, Object> data = Optional.ofNullable(response.get("data"))
                    .map(BraidObjects::<Map<String, Object>>cast)
                    .orElse(Collections.emptyMap());
            final List<Map> errorsMap = Optional.ofNullable(response.get("errors"))
                    .map(BraidObjects::<List<Map>>cast)
                    .orElse(Collections.emptyList());

            List<GraphQLError> errors = errorsMap.stream()
                    .map(val -> Optional.ofNullable(val)
                            .map(BraidObjects::<Map<String, Object>>cast)
                            .orElseThrow(IllegalArgumentException::new))
                    .map(MapGraphQLError::new)
                    .collect(Collectors.toList());
            return new DataFetcherResult<>(data, errors);
        });
    }
}
