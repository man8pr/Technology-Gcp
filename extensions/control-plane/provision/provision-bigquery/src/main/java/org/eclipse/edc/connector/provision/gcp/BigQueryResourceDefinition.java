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

import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.gcp.bigquery.service.BigQueryServiceSchema;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class BigQueryResourceDefinition extends ResourceDefinition {
    private Map<String, Object> properties = new HashMap<String, Object>();

    private BigQueryResourceDefinition() {
    }

    public String getProject() {
        return getString(BigQueryServiceSchema.PROJECT);
    }

    public String getDataset() {
        return getString(BigQueryServiceSchema.DATASET);
    }

    public String getTable() {
        return getString(BigQueryServiceSchema.TABLE);
    }

    public String getQuery() {
        return getString(BigQueryServiceSchema.QUERY);
    }

    public String getServiceAccountName() {
        return getString(BigQueryServiceSchema.SERVICE_ACCOUNT_NAME);
    }

    public String getCustomerName() {
        return getString(BigQueryServiceSchema.CUSTOMER_NAME);
    }

    private String getString(String key) {
        var value = Optional.ofNullable(properties.get(EDC_NAMESPACE + key)).orElseGet(() -> properties.get(key));
        if (value != null) {
            return (String) value;
        }

        return null;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder())
            .properties(properties);
    }

    public static class Builder extends ResourceDefinition.Builder<BigQueryResourceDefinition, Builder> {

        private Builder() {
            super(new BigQueryResourceDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder property(String key, String value) {
            resourceDefinition.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            resourceDefinition.properties = properties;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.getDataset(), BigQueryServiceSchema.DATASET);
            Objects.requireNonNull(resourceDefinition.getTable(), BigQueryServiceSchema.TABLE);
        }
    }
}
