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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.gcp.storage.GcsStoreSchema;

import static org.eclipse.edc.gcp.storage.GcsStoreSchema.BUCKET_NAME;
import static org.eclipse.edc.gcp.storage.GcsStoreSchema.LOCATION;
import static org.eclipse.edc.gcp.storage.GcsStoreSchema.SERVICE_ACCOUNT_EMAIL;
import static org.eclipse.edc.gcp.storage.GcsStoreSchema.SERVICE_ACCOUNT_NAME;
import static org.eclipse.edc.gcp.storage.GcsStoreSchema.STORAGE_CLASS;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@JsonDeserialize(builder = GcsProvisionedResource.Builder.class)
@JsonTypeName("dataspaceconnector:gcsgrovisionedresource")
public class GcsProvisionedResource extends ProvisionedDataDestinationResource {

    private GcsProvisionedResource() {
    }

    public String getBucketName() {
        return getDataAddress().getStringProperty(BUCKET_NAME);
    }

    public String getLocation() {
        return getDataAddress().getStringProperty(LOCATION);
    }

    public String getStorageClass() {
        return getDataAddress().getStringProperty(STORAGE_CLASS);
    }

    public String getServiceAccountName() {
        return getDataAddress().getStringProperty(SERVICE_ACCOUNT_NAME);
    }

    public String getServiceAccountEmail() {
        return getDataAddress().getStringProperty(SERVICE_ACCOUNT_EMAIL);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends
            ProvisionedDataDestinationResource.Builder<GcsProvisionedResource, GcsProvisionedResource.Builder> {

        private Builder() {
            super(new GcsProvisionedResource());
            dataAddressBuilder.type(GcsStoreSchema.TYPE);
        }

        @JsonCreator
        public static GcsProvisionedResource.Builder newInstance() {
            return new GcsProvisionedResource.Builder();
        }

        public GcsProvisionedResource.Builder bucketName(String bucketName) {
            dataAddressBuilder.property(EDC_NAMESPACE + BUCKET_NAME, bucketName);
            return this;
        }

        public GcsProvisionedResource.Builder location(String location) {
            dataAddressBuilder.property(EDC_NAMESPACE + LOCATION, location);
            return this;
        }

        public GcsProvisionedResource.Builder storageClass(String storageClass) {
            dataAddressBuilder.property(EDC_NAMESPACE + STORAGE_CLASS, storageClass);
            return this;
        }

        public GcsProvisionedResource.Builder serviceAccountName(String serviceAccountName) {
            dataAddressBuilder.property(EDC_NAMESPACE + SERVICE_ACCOUNT_NAME, serviceAccountName);
            return this;
        }

        public GcsProvisionedResource.Builder serviceAccountEmail(String serviceAccountEmail) {
            dataAddressBuilder.property(EDC_NAMESPACE + SERVICE_ACCOUNT_EMAIL, serviceAccountEmail);
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
