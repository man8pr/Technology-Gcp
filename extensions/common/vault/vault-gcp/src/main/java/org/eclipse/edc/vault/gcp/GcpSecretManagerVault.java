/*
 *  Copyright (c) 2023 Google LLC
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

package org.eclipse.edc.vault.gcp;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.api.gax.rpc.NotFoundException;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.secretmanager.v1.ProjectName;
import com.google.cloud.secretmanager.v1.Replication;
import com.google.cloud.secretmanager.v1.Secret;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;
import com.google.cloud.secretmanager.v1.SecretName;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.google.protobuf.ByteString;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * Vault extension implemented with GCP Secret Manager.
 */
public class GcpSecretManagerVault implements Vault {

    private final Monitor monitor;
    private final String project;
    private final String region;
    private final SecretManagerServiceClient secretManagerServiceClient;

    private static final String LATEST_VERSION_ALIAS = "latest"; // alias for the latest version of a Secret
    private static final int MAX_KEY_LENGTH = 255; // maximum Secret Manager key length
    private static final int HASH_LENGTH = 8; // Object.hashCode() returns int, which is 32 bit, in hex results in 8 char

    /** Messages used for exception handling. */
    private static final String SECRET_NOT_FOUND_MSG = "Secret not found or has no version ";
    private static final String RUNTIME_ERROR_MSG = "Runtime error ";
    private static final String EXCEPTION_MSG = "Exception ";
    private static final String SECRET_ALREADY_EXISTING_MSG = "Secret already exists ";
    private static final String RESOLVE_SECRET_FUNCTION = "resolving secret";
    private static final String STORE_SECRET_FUNCTION = "storing secrect";
    private static final String DELETE_SECRET_FUNCTION = "deleting secrect";


    /**
     * Factory helper constructing Vault object with default GCP credentials.
     *
     * @param monitor monitor object for logging.
     * @param project GCP project name.
     * @param region replica location for secrects created by the vault.
     * @return the created Vault object backed by Secret Manager.
     * @throws IOException if the creation of the Vault in GCP fails.
     */
    public static GcpSecretManagerVault createWithDefaultSettings(Monitor monitor, String project, String region) throws IOException {
        return new GcpSecretManagerVault(monitor, project, region, SecretManagerServiceClient.create());
    }

    /**
     * Factory helper constructing Vault object with service account GCP credentials.
     *
     * @param monitor monitor object for logging.
     * @param project GCP project name.
     * @param region replica location for secrects created by the vault.
     * @param credentialDataStream stream to service account credentials data.
     * @return the created Vault object backed by Secret Manager.
     * @throws IOException if the creation of the Vault in GCP fails.
     */
    public static GcpSecretManagerVault createWithServiceAccountCredentials(Monitor monitor, String project, String region, InputStream credentialDataStream) throws IOException {
        // TODO add proxy support.
        var serviceAccountCredentials = ServiceAccountCredentials.fromStream(credentialDataStream);
        var settings = SecretManagerServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(serviceAccountCredentials))
                    .build();
        return new GcpSecretManagerVault(monitor, project, region, SecretManagerServiceClient.create(settings));
    }

    /**
     * Vault object constructor.
     *
     * @param monitor monitor object for logging.
     * @param project GCP project name.
     * @param region replica location for secrects created by the vault.
     * @param secretClient GCP client to Secret Manager service (already authenticated).
     */
    public GcpSecretManagerVault(Monitor monitor, String project, String region, SecretManagerServiceClient secretClient) {
        this.monitor = monitor;
        this.project = project;
        this.region = region;
        this.secretManagerServiceClient = secretClient;
    }

    /**
     * Retrieves the secret stored under a specific key.
     *
     * @param key string key identifying the secret to be fetched.
     * @return the string if the secret is found, null otherwise.
     */
    @Override
    public @Nullable String resolveSecret(String key) {
        try {
            key = sanitizeKey(key);
            var secretVersionName = SecretVersionName.of(project, key, LATEST_VERSION_ALIAS);
            var response = secretManagerServiceClient.accessSecretVersion(secretVersionName);
            String payload = response.getPayload().getData().toStringUtf8();
            return payload;
        } catch (NotFoundException notFoundException) {
            handleException(RESOLVE_SECRET_FUNCTION, SECRET_NOT_FOUND_MSG + key + ": ", notFoundException);
            return null;
        } catch (RuntimeException runtimeException) {
            handleException(RESOLVE_SECRET_FUNCTION, RUNTIME_ERROR_MSG + key + ": ", runtimeException);
            return null;
        } catch (Exception exception) {
            handleException(RESOLVE_SECRET_FUNCTION, EXCEPTION_MSG + key, exception);
            return null;
        }
    }

    /**
     * Saves a secret stored under a specific key. If the selected key is already in use by a secrect, error is
     * returned and existing secret not overwritten.
     *
     * @param key string key identifying the secret to be stored.
     * @param value string value of the secret.
     * @return Result.success if the secret is stored.
     */
    @Override
    public Result<Void> storeSecret(String key, String value) {
        try {
            key = sanitizeKey(key);
            var secret =
                    Secret.newBuilder()
                    .setReplication(
                        Replication.newBuilder()
                        .setUserManaged(Replication.UserManaged.newBuilder()
                            // TODO add multi-region replica support?
                            .addReplicas(Replication.UserManaged.Replica.newBuilder()
                                .setLocation(region)
                                .build())
                            .build())
                        .build())
                    .build();

            var parent = ProjectName.of(project);
            var createdSecret = secretManagerServiceClient.createSecret(parent, key, secret);
            var payload = SecretPayload.newBuilder().setData(ByteString.copyFromUtf8(value)).build();
            var addedVersion = secretManagerServiceClient.addSecretVersion(createdSecret.getName(), payload);
            return Result.success();
        } catch (AlreadyExistsException alreadyExistsException) {
            return handleException(STORE_SECRET_FUNCTION, SECRET_ALREADY_EXISTING_MSG + key, alreadyExistsException);
        } catch (NotFoundException notFoundException) {
            return handleException(STORE_SECRET_FUNCTION, SECRET_NOT_FOUND_MSG + key, notFoundException);
        } catch (RuntimeException runtimeException) {
            return handleException(STORE_SECRET_FUNCTION, RUNTIME_ERROR_MSG + key, runtimeException);
        } catch (Exception exception) {
            return handleException(STORE_SECRET_FUNCTION, EXCEPTION_MSG + key, exception);
        }
    }

    /**
     * Deletes a secret stored under a specific key. If the selected key is not in use, error is returned.
     *
     * @param key string key identifying the secret to be deleted.
     * @return Result.success if the secret is deleted.
     */
    @Override
    public Result<Void> deleteSecret(String key) {
        try {
            key = sanitizeKey(key);
            var name = SecretName.of(project, key);
            secretManagerServiceClient.deleteSecret(name);
            return Result.success();
        } catch (NotFoundException notFoundException) {
            return handleException(DELETE_SECRET_FUNCTION, SECRET_NOT_FOUND_MSG + key, notFoundException);
        } catch (RuntimeException runtimeException) {
            return handleException(DELETE_SECRET_FUNCTION, RUNTIME_ERROR_MSG + key, runtimeException);
        } catch (Exception exception) {
            return handleException(DELETE_SECRET_FUNCTION, EXCEPTION_MSG + key, exception);
        }
    }

    /**
     * Checks if the given key parameter fits GCP requirements, if not it will:
     * - any invalid character is replaced with dash '-' char.
     * - if the key is too long it is truncated to max 255 chars.
     * - if the key is changed in the process, original key is hashed into a 8-chars string appended at the end of
     *   the returned key.
     *
     * Secret key is a string with a maximum length of 255 characters and can contain uppercase and lowercase letters,
     * digits, and the hyphen (`-`) and underscore (`_`) characters.
     *
     * @param key string key to be checked and sanitized.
     * @return sanitized key.
     */
    String sanitizeKey(String key) {
        boolean modified = false;
        var originalKey = key;

        var sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            var c = key.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                modified = true;
                sb.append('-');
            } else {
                sb.append(c);
            }

            if (sb.length() > MAX_KEY_LENGTH ||
                    (modified && sb.length() > (MAX_KEY_LENGTH - HASH_LENGTH - 1))) {
                sb.setLength(MAX_KEY_LENGTH - HASH_LENGTH - 1);
                modified = true;
                i = key.length();
            }
        }

        if (modified) {
            var originalKeyHash = String.format("%08X", originalKey.hashCode());
            var fixedKey = sb.append('_').append(originalKeyHash).toString();
            monitor.warning("GCP Secret Manager vault sanitized the key, original:" + originalKey + " fixed:" + fixedKey);
            return fixedKey;
        }

        return key;
    }

    private Result<Void> handleException(String function, String message, Exception exception) {
        if (exception.getClass() == RuntimeException.class) {
            monitor.severe(message, exception);
        } else {
            monitor.debug(message, exception);
        }
        return Result.failure("(" + function + ")" + message + ": " + exception.getMessage());
    }
}
