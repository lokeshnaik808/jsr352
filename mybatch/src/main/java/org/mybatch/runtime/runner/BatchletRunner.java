/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mybatch.runtime.runner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.mybatch.job.Batchlet;
import org.mybatch.metadata.ApplicationMetaData;
import org.mybatch.operations.JobOperatorImpl;
import org.mybatch.runtime.context.StepContextImpl;
import org.mybatch.util.BatchUtil;

import static org.mybatch.util.BatchLogger.LOGGER;

public class BatchletRunner implements Callable<Void> {
    private Batchlet batchlet;
    private StepExecutionRunner stepExecutionRunner;
    private StepContextImpl stepContext;

    public BatchletRunner(Batchlet batchlet, StepExecutionRunner stepExecutionRunner) {
        this.batchlet = batchlet;
        this.stepExecutionRunner = stepExecutionRunner;
        this.stepContext = stepExecutionRunner.getStepContext();
    }

    @Override
    public Void call() {
        Thread thread = Thread.currentThread();
        ClassLoader originalCL = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(BatchUtil.getBatchApplicationClassLoader());
            String ref = batchlet.getRef();
            ApplicationMetaData appData = stepExecutionRunner.getJobExecutionRunner().getJobInstance().getApplicationMetaData();
            Map<String, ApplicationMetaData> m = new HashMap<String, ApplicationMetaData>();
            m.put(ApplicationMetaData.class.getName(), appData);

            try {
                Object artifactObj = stepExecutionRunner.getJobExecutionRunner().getJobInstance().getArtifactFactory().create(ref, m);
                StepExecutionRunner.invokeFunctionMethods(artifactObj, StepExecutionRunner.methodAnnotations);
            } catch (Throwable e) {
                LOGGER.failToRunBatchlet(e, batchlet);
                stepContext.setBatchStatus(JobOperatorImpl.BatchStatus.FAILED.name());
            }
            return null;
        } finally {
            thread.setContextClassLoader(originalCL);
        }
    }
}
