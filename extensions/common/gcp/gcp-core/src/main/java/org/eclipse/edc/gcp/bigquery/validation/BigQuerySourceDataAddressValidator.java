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

package org.eclipse.edc.gcp.bigquery.validation;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.ArrayList;

import static org.eclipse.edc.gcp.bigquery.service.BigQueryServiceSchema.QUERY;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;

public class BigQuerySourceDataAddressValidator implements Validator<DataAddress> {
    @Override
    public ValidationResult validate(DataAddress input) {
        var violations = new ArrayList<Violation>();

        if (StringUtils.isNullOrBlank(input.getStringProperty(QUERY, null))) {
            violations.add(Violation.violation("Must have a %s property".formatted(QUERY), QUERY));
        }

        if (!violations.isEmpty()) {
            return failure(violations);
        }
        return success();
    }
}
