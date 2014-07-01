/*
 * Copyright 2014 Signal.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.signal.loadgen;

import java.beans.PropertyDescriptor;

import org.apache.jmeter.testbeans.BeanInfoSupport;

/**
 * @author codyaray
 * @since 6/30/14
 */
public class TagserveLoadGeneratorBeanInfo extends BeanInfoSupport {

  private static final String FILENAME = "filename";
  private static final String VARIABLE_NAME = "variableName";

  public TagserveLoadGeneratorBeanInfo() {
    super(TagserveLoadGenerator.class);

    createPropertyGroup("tagserve_load_generator", new String[] {
        FILENAME, VARIABLE_NAME
    });

    PropertyDescriptor p = property(FILENAME);
    p.setValue(NOT_UNDEFINED, Boolean.TRUE);
    p.setValue(DEFAULT, "");
    p.setValue(NOT_EXPRESSION, Boolean.TRUE);

    p = property(VARIABLE_NAME);
    p.setValue(NOT_UNDEFINED, Boolean.TRUE);
    p.setValue(DEFAULT, "");
    p.setValue(NOT_EXPRESSION, Boolean.TRUE);
  }

}
