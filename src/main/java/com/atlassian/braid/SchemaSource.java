package com.atlassian.braid;

import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.List;

/**
 * A data source that contains a schema to be merged via {@link BraidSchema}
 */
public interface SchemaSource extends BatchLoaderFactory {

    /**
     * @return the public schema document to be braided
     */
    TypeDefinitionRegistry getSchema();

    /**
     * @return the full schema exposed by the source for use in links
     * @since 0.6.0
     */
    default TypeDefinitionRegistry getPrivateSchema() {
        return getSchema();
    }

    /**
     * @return the data source identifier to be used in links targeting this data source.
     * @see com.atlassian.braid.Link#getTargetNamespace()
     */
    SchemaNamespace getNamespace();

    /**
     * @return a list of links that connect fields in this data source to other data sources
     */
    List<Link> getLinks();
}
