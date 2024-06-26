/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LLC - Initial implementation
 *
 */

package org.eclipse.edc.connector.provision.gcp;


import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;

import java.util.Objects;

public class GcsResourceDefinition extends ResourceDefinition {

    private String location;
    private String storageClass;
    private String bucketName;
    private String serviceAccountName;

    private GcsResourceDefinition() {
    }

    public String getLocation() {
        return this.location;
    }

    public String getStorageClass() {
        return this.storageClass;
    }

    public String getBucketName() {
        return this.bucketName;
    }

    public String getServiceAccountName() {
        return this.serviceAccountName;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder())
                .location(location)
                .storageClass(storageClass)
                .bucketName(bucketName)
                .serviceAccountName(serviceAccountName);
    }

    public static class Builder extends ResourceDefinition.Builder<GcsResourceDefinition, Builder> {

        private Builder() {
            super(new GcsResourceDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder location(String location) {
            resourceDefinition.location = location;
            return this;
        }

        public Builder storageClass(String storageClass) {
            resourceDefinition.storageClass = storageClass;
            return this;
        }

        public Builder bucketName(String bucketName) {
            resourceDefinition.bucketName = bucketName;
            return this;
        }

        public Builder serviceAccountName(String serviceAccountName) {
            resourceDefinition.serviceAccountName = serviceAccountName;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            // Bucket name is not required: if not present, provisioner generates a new one
            // using the transfer id.
            Objects.requireNonNull(resourceDefinition.location, "location");
            Objects.requireNonNull(resourceDefinition.storageClass, "storageClass");
        }
    }
}
