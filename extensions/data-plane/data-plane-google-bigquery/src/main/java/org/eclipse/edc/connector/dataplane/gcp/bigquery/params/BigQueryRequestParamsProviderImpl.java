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

package org.eclipse.edc.connector.dataplane.gcp.bigquery.params;

import org.eclipse.edc.gcp.bigquery.BigQueryDataAddress;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

public class BigQueryRequestParamsProviderImpl implements BigQueryRequestParamsProvider {
    public BigQueryRequestParamsProviderImpl() {
    }

    private BigQueryRequestParams.Builder getParamsBuilder(DataAddress address) {
        var bqAddress = BigQueryDataAddress.Builder.newInstance()
                .copyFrom(address)
                .build();
        return BigQueryRequestParams.Builder.newInstance()
            .project(bqAddress.getProject())
            .dataset(bqAddress.getDataset())
            .table(bqAddress.getTable())
            .query(bqAddress.getQuery())
            .serviceAccountName(bqAddress.getServiceAccountName());
    }

    @Override
    public BigQueryRequestParams provideSourceParams(DataFlowStartMessage message) {
        var bqAddress = BigQueryDataAddress.Builder.newInstance()
                .copyFrom(message.getSourceDataAddress())
                .build();
        return getParamsBuilder(message.getSourceDataAddress())
            .sinkAddress(message.getDestinationDataAddress())
            .destinationTable(bqAddress.getDestinationTable())
            .build();
    }

    @Override
    public BigQueryRequestParams provideSinkParams(DataFlowStartMessage message) {
        return getParamsBuilder(message.getDestinationDataAddress()).build();
    }
}
