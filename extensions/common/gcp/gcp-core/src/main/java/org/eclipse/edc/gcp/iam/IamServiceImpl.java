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

package org.eclipse.edc.gcp.iam;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.iam.admin.v1.IAMClient;
import com.google.cloud.iam.credentials.v1.GenerateAccessTokenRequest;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.ServiceAccountName;
import com.google.protobuf.Duration;
import org.eclipse.edc.gcp.common.GcpAccessToken;
import org.eclipse.edc.gcp.common.GcpConfiguration;
import org.eclipse.edc.gcp.common.GcpException;
import org.eclipse.edc.gcp.common.GcpServiceAccount;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class IamServiceImpl implements IamService {
    private static final long ONE_HOUR_IN_S = TimeUnit.HOURS.toSeconds(1);
    private final Monitor monitor;
    private final GcpConfiguration gcpConfiguration;
    private Supplier<IAMClient> iamClientSupplier;
    private Supplier<IamCredentialsClient> iamCredentialsClientSupplier;
    private CredentialsManager credentialsManager;

    private IamServiceImpl(Monitor monitor, GcpConfiguration gcpConfiguration) {
        this.monitor = monitor;
        this.gcpConfiguration = gcpConfiguration;
    }

    @Override
    public GcpServiceAccount getServiceAccount(String serviceAccountName) {
        if (serviceAccountName == null && gcpConfiguration.serviceAccountName() == null) {
            return ADC_SERVICE_ACCOUNT;
        }

        if (serviceAccountName == null) {
            serviceAccountName = gcpConfiguration.serviceAccountName();
        }

        try (var client = iamClientSupplier.get()) {
            var serviceAccountEmail = getServiceAccountEmail(serviceAccountName, gcpConfiguration.projectId());
            var name = ServiceAccountName.of(gcpConfiguration.projectId(), serviceAccountEmail).toString();
            var response = client.getServiceAccount(name);

            return new GcpServiceAccount(response.getEmail(), response.getName(), response.getDescription());
        } catch (ApiException e) {
            if (e.getStatusCode().getCode() == StatusCode.Code.NOT_FOUND) {
                monitor.severe("Service account '" + serviceAccountName + "'not found", e);
                throw new GcpException("Service account '" + serviceAccountName + "'not found", e);
            }
            monitor.severe("Unable to get service account '" + serviceAccountName + "'", e);
            throw new GcpException("Unable to get service account '" + serviceAccountName + "'", e);
        }
    }

    @Override
    public GcpAccessToken createAccessToken(GcpServiceAccount serviceAccount, String... scopes) {
        if (serviceAccount.equals(ADC_SERVICE_ACCOUNT)) {
            var credentials = credentialsManager.getApplicationDefaultCredentials()
                    .createScoped(scopes);
            credentialsManager.refreshCredentials(credentials);
            var token = credentials.getAccessToken();
            return new GcpAccessToken(token.getTokenValue(), token.getExpirationTime().getTime());
        }

        try (var iamCredentialsClient = iamCredentialsClientSupplier.get()) {
            var name = ServiceAccountName.of("-", serviceAccount.getEmail());
            var lifetime = Duration.newBuilder().setSeconds(ONE_HOUR_IN_S).build();
            var request = GenerateAccessTokenRequest.newBuilder()
                    .setName(name.toString())
                    .addAllScope(Arrays.asList(scopes))
                    .setLifetime(lifetime)
                    .build();
            var response = iamCredentialsClient.generateAccessToken(request);
            monitor.debug("Created access token for " + serviceAccount.getEmail());
            var expirationMillis = response.getExpireTime().getSeconds() * 1000;
            return new GcpAccessToken(response.getAccessToken(), expirationMillis);
        } catch (Exception e) {
            throw new GcpException("Error creating service account token:\n" + e);
        }
    }

    @Override
    public GoogleCredentials getCredentials(GcpAccessToken accessToken) {
        return GoogleCredentials.create(
                new AccessToken(accessToken.getToken(), new Date(accessToken.getExpiration()))
        );
    }

    @Override
    public GoogleCredentials getCredentials(GcpServiceAccount serviceAccount, String... scopes) {
        var sourceCredentials = credentialsManager.getApplicationDefaultCredentials();
        credentialsManager.refreshCredentials(sourceCredentials);

        if (serviceAccount.equals(ADC_SERVICE_ACCOUNT)) {
            var adcCredentials = sourceCredentials.createScoped(scopes);
            monitor.debug(
                    "Credentials for project '" + gcpConfiguration.projectId() + "' using ADC");
            return adcCredentials;
        }

        sourceCredentials = sourceCredentials.createScoped(IAM_SCOPE);
        monitor.debug("Credentials for project '" + gcpConfiguration.projectId() +
                "' using service account '" + serviceAccount.getName() + "'");

        return credentialsManager.createImpersonated(
            sourceCredentials,
            serviceAccount,
            3600,
            scopes);
    }

    private String getServiceAccountEmail(String name, String project) {
        return String.format("%s@%s.iam.gserviceaccount.com", name, project);
    }

    public static class Builder {
        private IamServiceImpl iamServiceImpl;

        private Builder(Monitor monitor, GcpConfiguration gcpConfiguration) {
            iamServiceImpl = new IamServiceImpl(monitor, gcpConfiguration);
        }

        public static IamServiceImpl.Builder newInstance(Monitor monitor, GcpConfiguration gcpConfiguration) {
            return new Builder(monitor, gcpConfiguration);
        }

        public Builder iamClientSupplier(Supplier<IAMClient> iamClientSupplier) {
            iamServiceImpl.iamClientSupplier = iamClientSupplier;
            return this;
        }

        public Builder iamCredentialsClientSupplier(Supplier<IamCredentialsClient> iamCredentialsClientSupplier) {
            iamServiceImpl.iamCredentialsClientSupplier = iamCredentialsClientSupplier;
            return this;
        }

        public Builder credentialUtil(CredentialsManager credentialUtil) {
            iamServiceImpl.credentialsManager = credentialUtil;
            return this;
        }

        public IamServiceImpl build() {
            Objects.requireNonNull(iamServiceImpl.gcpConfiguration, "gcpConfiguration");
            Objects.requireNonNull(iamServiceImpl.monitor, "monitor");

            if (iamServiceImpl.iamClientSupplier == null) {
                iamServiceImpl.iamClientSupplier = defaultIamClientSupplier();
            }
            if (iamServiceImpl.iamCredentialsClientSupplier == null) {
                iamServiceImpl.iamCredentialsClientSupplier = defaultIamCredentialsClientSupplier();
            }

            if (iamServiceImpl.credentialsManager == null) {
                iamServiceImpl.credentialsManager = new DefaultCredentialsManager(iamServiceImpl.monitor);
            }

            return iamServiceImpl;
        }

        /**
         * Supplier of {@link IAMClient} using application default credentials
         */
        private Supplier<IAMClient> defaultIamClientSupplier() {
            return () -> {
                try {
                    return IAMClient.create();
                } catch (IOException e) {
                    throw new GcpException("Error while creating IAMClient", e);
                }
            };
        }

        /**
         * Supplier of {@link IamCredentialsClient} using application default credentials
         */
        private Supplier<IamCredentialsClient> defaultIamCredentialsClientSupplier() {
            return () -> {
                try {
                    return IamCredentialsClient.create();
                } catch (IOException e) {
                    throw new GcpException("Error while creating IamCredentialsClient", e);
                }
            };
        }
    }
}
