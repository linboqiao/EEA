/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.uima.util.NewsNameComparators;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem,
 * sort them by the offset
 */
public class OffsetOnlySortedXmiCollectionReader extends CollectionReader_ImplBase {
    /**
     * Name of configuration parameter that must be set to the path of a directory containing the XMI
     * files.
     */
    public static final String PARAM_INPUTDIR = "InputDirectory";

    /**
     * Name of the configuration parameter that must be set to indicate if the execution fails if an
     * encountered type is unknown
     */
    public static final String PARAM_FAILUNKNOWN = "FailOnUnknownType";

    public static final String PARAM_FILE_SUFFIX = "fileSuffix";

    public static final String DEFAULT_FILE_SUFFIX = ".xmi";

    private Boolean mFailOnUnknownType;

    private ArrayList<File> mFiles;

    private int mCurrentIndex;

    /**
     * @see CollectionReader_ImplBase#initialize()
     */
    public void initialize() throws ResourceInitializationException {
        mFailOnUnknownType = (Boolean) getConfigParameterValue(PARAM_FAILUNKNOWN);
        if (null == mFailOnUnknownType) {
            mFailOnUnknownType = true; // default to true if not specified
        }

        String inputFileSuffix = (String) getConfigParameterValue(PARAM_FILE_SUFFIX);
        if (StringUtils.isEmpty(inputFileSuffix)) {
            inputFileSuffix = DEFAULT_FILE_SUFFIX;
        }


        File directory = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());
        mCurrentIndex = 0;

        // if input directory does not exist or is not a directory, throw exception
        if (!directory.exists() || !directory.isDirectory()) {
            throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND,
                    new Object[]{PARAM_INPUTDIR, this.getMetaData().getName(), directory.getPath()});
        }

        // get list of .xmi files in the specified directory
        mFiles = new ArrayList<File>();
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isDirectory() && files[i].getName().endsWith(inputFileSuffix)) {
                mFiles.add(files[i]);
            }
        }

        Collections.sort(mFiles, NewsNameComparators.getFileOffsetComparator(inputFileSuffix));
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#hasNext()
     */
    public boolean hasNext() {
        return mCurrentIndex < mFiles.size();
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#getNext(CAS)
     */
    public void getNext(CAS aCAS) throws IOException, CollectionException {
        File currentFile = (File) mFiles.get(mCurrentIndex++);
        FileInputStream inputStream = new FileInputStream(currentFile);
        try {
            XmiCasDeserializer.deserialize(inputStream, aCAS, !mFailOnUnknownType);
        } catch (SAXException e) {
            throw new CollectionException(e);
        } finally {
            inputStream.close();
        }
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
     */
    public void close() throws IOException {
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
     */
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(mCurrentIndex, mFiles.size(), Progress.ENTITIES)};
    }

}
