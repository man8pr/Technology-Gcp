/*
 *  Copyright (c) 2024 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LLC
 *
 */

package org.eclipse.edc.connector.provision.gcp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.gcp.bigquery.service.BigQueryServiceSchema;

import java.util.Map;

@JsonDeserialize(builder = BigQueryProvisionedResource.Builder.class)
@JsonTypeName("dataspaceconnector:bigqueryprovisionedresource")
public class BigQueryProvisionedResource extends ProvisionedDataDestinationResource {
    private BigQueryProvisionedResource() {
    }

    public String getQuery() {
        return getDataAddress().getStringProperty(BigQueryServiceSchema.QUERY);
    }

    public String getTable() {
        return getDataAddress().getStringProperty(BigQueryServiceSchema.TABLE);
    }

    public String getDataset() {
        return getDataAddress().getStringProperty(BigQueryServiceSchema.DATASET);
    }

    public String getServiceAccountName() {
        return getDataAddress().getStringProperty(BigQueryServiceSchema.SERVICE_ACCOUNT_NAME);
    }

    public String getProject() {
        return getDataAddress().getStringProperty(BigQueryServiceSchema.PROJECT);
    }

    public String getCustomerName() {
        return getDataAddress().getStringProperty(BigQueryServiceSchema.CUSTOMER_NAME);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends
            ProvisionedDataDestinationResource.Builder<BigQueryProvisionedResource, BigQueryProvisionedResource.Builder> {

        private Builder() {
            super(new BigQueryProvisionedResource());
            dataAddressBuilder.type(BigQueryServiceSchema.BIGQUERY_DATA);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder properties(Map<String, Object> properties) {
            dataAddressBuilder.properties(properties);
            return this;
        }

        public Builder query(String query) {
            dataAddressBuilder.property(BigQueryServiceSchema.QUERY, query);
            return this;
        }

        public Builder project(String project) {
            dataAddressBuilder.property(BigQueryServiceSchema.PROJECT, project);
            return this;
        }

        public Builder table(String table) {
            dataAddressBuilder.property(BigQueryServiceSchema.TABLE, table);
            return this;
        }

        public Builder dataset(String dataset) {
            dataAddressBuilder.property(BigQueryServiceSchema.DATASET, dataset);
            return this;
        }

        public Builder serviceAccountName(String serviceAccountName) {
            dataAddressBuilder.property(BigQueryServiceSchema.SERVICE_ACCOUNT_NAME, serviceAccountName);
            return this;
        }

        public Builder customerName(String customerName) {
            dataAddressBuilder.property(BigQueryServiceSchema.CUSTOMER_NAME, customerName);
            return this;
        }

        @Override
        public Builder resourceName(String name) {
            dataAddressBuilder.keyName(name);
            super.resourceName(name);
            return this;
        }
    }
}
