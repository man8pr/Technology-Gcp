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

package org.eclipse.edc.gcp.storage;

import org.eclipse.edc.gcp.common.GcpServiceAccount;
import org.eclipse.edc.gcp.common.GcsBucket;

/**
 * Wrapper around Google Cloud Storage API for decoupling.
 */
public interface StorageService {

    /**
     * Checks if a bucket with the given name exists in the specific location,
     * and returns it. Otherwise, if no bucket with the given name exists,
     * creates it.
     *
     * @param bucketName The name of the bucket, must be unique in GCP
     * @param location   The location of the bucket (e.g. "EUROPE-WEST3", "EU")
     * @return {@link GcsBucket}
     */
    GcsBucket getOrCreateBucket(String bucketName, String location);

    /**
     * Attaches a new role binding to the bucket that grants the service account the specified role on the bucket
     *
     * @param bucket         The bucket to which the role binding should be attached to
     * @param serviceAccount The service account that should be granted the role on the bucket
     * @param role           The role that should be granted
     */
    void addRoleBinding(GcsBucket bucket, GcpServiceAccount serviceAccount, String role);

    /**
     * Grants the service account, that will be used by the data provider, the required permissions to upload data to the bucket
     *
     * @param bucket         The bucket to which the service account will be granted permissions
     * @param serviceAccount The service account that should be granted the permissions on the bucket
     */
    void addProviderPermissions(GcsBucket bucket, GcpServiceAccount serviceAccount);

    /**
     * Deletes the bucket if it exists.
     *
     * @param bucketName The name of the bucket
     * @return true if the bucket has been deleted and false if not
     */
    boolean deleteBucket(String bucketName);

    /**
     * Checks if the bucket is empty.
     *
     * @param bucketName The name of the bucket
     * @return true if the bucket is empty and false if not
     */
    boolean isEmpty(String bucketName);
}
