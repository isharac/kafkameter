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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.joda.time.Interval;

import com.onetag.metrics.TagRequestMetrics;
import com.onetag.metrics.TagRequestMetrics.TagFires;
import com.onetag.metrics.impl.TagFiresImpl;
import com.onetag.metrics.impl.TagRequestMetricsImpl;
import com.onetag.metrics.json.JSONTagRequestMetricsMarshaller;

/**
 * Config Element which reads a Tagserve Synthetic Load Description from a file, generates
 * JSON TagRequestMetrics messages, and exports them under a given variableName.
 *
 * @author codyaray
 * @since 6/27/14
 */
public class TagserveLoadGenerator extends ConfigTestElement implements TestBean, LoopIterationListener {

  private static final Logger log = LoggingManager.getLoggerForClass();

  private static final Type SITE_CONFIGS_TYPE = new TypeToken<Map<String, SiteConfig>>(){}.getType();

  private final JSONTagRequestMetricsMarshaller marshaller = new JSONTagRequestMetricsMarshaller();
  private final Random random = new Random();

  private Map<String, SiteConfig> configs = null;
  private Map<String, Double> siteWeights = null;

  private String filename;
  private String variableName;

  @Override
  public void iterationStart(LoopIterationEvent loopIterationEvent) {
    if (configs == null) {
      configs = readSiteConfigs(getFilename());
      siteWeights = parseSiteWeights(configs);
    }
    JMeterVariables variables = JMeterContextService.getContext().getVariables();
    variables.put(getVariableName(), nextMessage());
  }

  private String nextMessage() {
    return marshaller.marshal(nextMetrics());
  }

  private TagRequestMetrics nextMetrics() {
    String siteId = nextSiteId();
    SiteConfig siteConfig = configs.get(siteId);
    Interval requestInterval = nextInterval();
    ImmutableSet<Long> pageIds = nextPages(siteConfig);
    ImmutableMap<Long, TagFires> tagMetrics = toTagMetrics(nextTags(siteConfig, pageIds));
    return new TagRequestMetricsImpl(siteId, requestInterval, null, false, false, pageIds, tagMetrics);
  }

  private Interval nextInterval() {
//    JMeterVariables variables = JMeterContextService.getContext().getVariables();
//    int iteration = variables.getIteration();
    long now = System.currentTimeMillis();
    return new Interval(now, now + 30);
  }

  private String nextSiteId() {
    // TODO: replace this linear search with binary search
    double r = random.nextDouble();
    for (Map.Entry<String, Double> entry : siteWeights.entrySet()) {
      if (r < entry.getValue()) {
        return entry.getKey();
      }
    }
    // Unreachable if siteWeights is created correctly
    return null;
  }

  private ImmutableSet<Long> nextPages(SiteConfig siteConfig) {
    ImmutableSet.Builder<Long> pages = ImmutableSet.builder();
    for (Map.Entry<String, PageConfig> entry : siteConfig.getPages().entrySet()) {
      PageConfig pageConfig = entry.getValue();
      double r = random.nextDouble();
      if (r < pageConfig.getWeight()) {
        pages.add(Long.parseLong(entry.getKey()));
      }
    }
    return pages.build();
  }

  private ImmutableSet<Long> nextTags(SiteConfig siteConfig, Set<Long> pages) {
    ImmutableSet.Builder<Long> tags = ImmutableSet.builder();
    Map<String, PageConfig> pageConfigs = siteConfig.getPages();
    for (Long pageId : pages) {
      PageConfig pageConfig = pageConfigs.get(String.valueOf(pageId));
      tags.addAll(pageConfig.getTags());
    }
    return tags.build();
  }

  private ImmutableMap<Long, TagFires> toTagMetrics(Set<Long> tagIds) {
    ImmutableMap.Builder<Long, TagFires> tagMetrics = ImmutableMap.builder();
    for (Long tagId : tagIds) {
      tagMetrics.put(tagId, new TagFiresImpl(1, 0, 0));
    }
    return tagMetrics.build();
  }

  private static Map<String, Double> parseSiteWeights(Map<String, SiteConfig> configs) {
    double sum = 0;
    ImmutableMap.Builder<String, Double> siteWeights = ImmutableMap.builder();
    for (Map.Entry<String, SiteConfig> entry : configs.entrySet()) {
      sum += entry.getValue().getWeight();
      siteWeights.put(entry.getKey(), sum);
    }
    if (sum != 1) {
      throw new RuntimeException("Site weights must sum to unity");
    }
    return siteWeights.build();
  }

  private static Map<String, SiteConfig> readSiteConfigs(String filename) {
    Gson gson = new Gson();
    File file = new File(filename);
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
      try {
        return gson.fromJson(bufferedReader, SITE_CONFIGS_TYPE);
      } finally {
        bufferedReader.close();
      }
    } catch (FileNotFoundException e) {
      log.fatalError("Config file not found " + e);
      Throwables.propagate(e);
    } catch (JsonParseException e) {
      log.fatalError("Problem parsing json from " + file + " " + e);
      Throwables.propagate(e);
    } catch (IOException e) {
      log.fatalError("Unable to retrieve config from: " + file + " " + e);
      Throwables.propagate(e);
    }
    return ImmutableMap.of();
  }

  /**
   * @return the variableName
   */
  public synchronized String getVariableName() {
    return variableName;
  }

  /**
   * @param variableName the variableName to set
   */
  public synchronized void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  /**
   * @return the filename
   */
  public synchronized String getFilename() {
    return filename;
  }

  /**
   * @param filename the filename to set
   */
  public synchronized void setFilename(String filename) {
    this.filename = filename;
  }

  /**
   * Helper for testing outside of JMeter
   */
  public static void main(String[] args) {
    TagserveLoadGenerator loadgen = new TagserveLoadGenerator();

    // Mock out JMeter environment
    JMeterVariables variables = new JMeterVariables();
    JMeterContextService.getContext().setVariables(variables);
    loadgen.setFilename("config1.json");
    loadgen.setVariableName("kafka_message");

    loadgen.iterationStart(null);

    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      System.out.println(entry.getKey() + " : " + entry.getValue());
    }
  }
}
