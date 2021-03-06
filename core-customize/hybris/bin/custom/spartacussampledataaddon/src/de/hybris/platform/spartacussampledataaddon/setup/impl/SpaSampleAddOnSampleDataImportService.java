/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 * The files in this addon are licensed under the Apache Software License, v. 2 
 * except as noted otherwise in the LICENSE file.
 */
package de.hybris.platform.spartacussampledataaddon.setup.impl;

import de.hybris.platform.addonsupport.setup.impl.DefaultAddonSampleDataImportService;
import de.hybris.platform.catalog.jalo.SyncItemCronJob;
import de.hybris.platform.catalog.jalo.SyncItemJob;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.catalog.model.SyncItemJobModel;
import de.hybris.platform.core.initialization.SystemSetupContext;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.List;

import org.springframework.beans.factory.annotation.Required;


/**
 * This class extends {@link DefaultAddonSampleDataImportService} and specifies how to import sample data spartacus
 */
@SuppressWarnings("deprecation")
public class SpaSampleAddOnSampleDataImportService extends DefaultAddonSampleDataImportService
{
	private static final String SYNC_CONTENT_CATALOG = "electronics->spa";

	private ModelService modelService;

	@Override
	protected void importContentCatalog(final SystemSetupContext context, final String importRoot, final String catalogName)
	{
		// 1- create new catalog
		importImpexFile(context, importRoot + "/contentCatalogs/" + catalogName + "ContentCatalog/catalog.impex", false);

		// 2- sync xxxContentCatalog:Staged->xxx-spaContentCatalog:Staged
		final CatalogVersionModel catalog = getCatalogVersionService().getCatalogVersion(catalogName + "-spaContentCatalog", "Staged");
		List<SyncItemJobModel> synItemsJobs = catalog.getIncomingSynchronizations();
		if (synItemsJobs.size() > 0)
		{
			SyncItemJobModel job = synItemsJobs.get(0);
			final SyncItemJob jobItem = getModelService().getSource(job);
			synchronizeSpaContentCatalog(context, jobItem);
		}

		// 3- perform some cleaning
		importImpexFile(context, importRoot + "/contentCatalogs/" + catalogName + "ContentCatalog/cleaning.impex", false);
		

		// 4- import content catalog from impex
		super.importContentCatalog(context, importRoot, catalogName);

		// 5- synchronize spaContentCatalog:staged->online
		synchronizeContentCatalog(context, catalogName + "-spa", true);

		// 6- give permission to cmsmanager to do the sync
		importImpexFile(context, importRoot + "/contentCatalogs/" + catalogName + "ContentCatalog/sync.impex", false);

		// 7- import email data
		importImpexFile(context, importRoot + "/contentCatalogs/" + catalogName + "ContentCatalog/email-content.impex", false);
			
		// 8- import sample data for testing
		importImpexFile(context, importRoot + "/contentCatalogs/" + catalogName + "ContentCatalog/test-data.impex", false);
		
	}

	private void synchronizeSpaContentCatalog(final SystemSetupContext context, SyncItemJob syncJobItem)
	{
		logInfo(context, "Begin synchronizing Content Catalog [" + SYNC_CONTENT_CATALOG + "] - synchronizing");

		final SyncItemCronJob syncCronJob = syncJobItem.newExecution();
		syncCronJob.setLogToDatabase(false);
		syncCronJob.setLogToFile(false);
		syncCronJob.setForceUpdate(false);
		syncJobItem.configureFullVersionSync(syncCronJob);

		logInfo(context, "Starting synchronization, this may take a while ...");
		syncJobItem.perform(syncCronJob, true);

		logInfo(context, "Synchronization complete for catalog [" + SYNC_CONTENT_CATALOG + "]");
		final CronJobResult result = modelService.get(syncCronJob.getResult());
		final CronJobStatus status = modelService.get(syncCronJob.getStatus());

		PerformResult syncCronJobResult = new PerformResult(result, status);
		if (isSyncRerunNeeded(syncCronJobResult))
		{
			logInfo(context, "Catalog catalog [" + SYNC_CONTENT_CATALOG + "] sync has issues.");
		}

		logInfo(context, "Done synchronizing  Content Catalog [" + SYNC_CONTENT_CATALOG + "]");
	}

	protected ModelService getModelService()
	{
		return modelService;
	}

	@Required
	public void setModelService(ModelService modelService)
	{
		this.modelService = modelService;
	}
}
