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

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.gcp.common.GcpAccessToken;
import org.eclipse.edc.gcp.common.GcpConfiguration;
import org.eclipse.edc.gcp.iam.IamService;
import org.eclipse.edc.gcp.storage.StorageServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

public class GcsProvisionExtension implements ServiceExtension {
    @Inject
    private ProvisionManager provisionManager;

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Override
    public String name() {
        return "GCP storage provisioner";
    }

    @Inject
    private IamService iamService;

    @Inject
    private GcpConfiguration gcpConfiguration;

    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var storageClient = createDefaultStorageClient(gcpConfiguration.projectId());
        var storageService = new StorageServiceImpl(storageClient, monitor);

        var provisioner = new GcsProvisioner(gcpConfiguration, monitor, storageService, iamService);
        provisionManager.register(provisioner);

        manifestGenerator.registerGenerator(new GcsConsumerResourceDefinitionGenerator());

        typeManager.registerTypes(GcsProvisionedResource.class, GcsResourceDefinition.class, GcpAccessToken.class);
    }


    /**
     * Creates {@link Storage} for the specified project using application default credentials
     *
     * @param projectId The project that should be used for storage operations
     * @return {@link Storage}
     */
    private Storage createDefaultStorageClient(String projectId) {
        return StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    }
}