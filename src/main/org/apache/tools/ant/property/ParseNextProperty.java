/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.property;

import java.text.ParsePosition;

import org.apache.tools.ant.Project;

/**
 * Interface to parse a property.
 * @since Ant 1.8.0
 */
public interface ParseNextProperty {
    /**
     * Get the current project.
     * @return the current ant project.
     */
    Project getProject();

    /**
     * Return any property that can be parsed from the specified position
     * in the specified String.
     * @param value String to parse
     * @param pos ParsePosition
     * @return Object or null if no property is at the current location.
     */
    Object parseNextProperty(String value, ParsePosition pos);
}
