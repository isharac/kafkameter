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
package co.signal.loadgen.example;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import co.signal.loadgen.SyntheticLoadGenerator;

/**
 * Example {@link SyntheticLoadGenerator} which generates {@link TagRequestMetrics} messages
 * as {@link TagRequestMetricsJsonMarshaller JSON} according to a distribution given by an
 * example Tagserve Synthetic Load Description. This example Load Description is documented
 * in the project README.
 *
 * @author codyaray
 * @since 7/17/2014
 */
public class TagserveLoadGenerator implements SyntheticLoadGenerator {

  private static final Logger log = LoggingManager.getLoggerForClass();

  private static final Type SITE_CONFIGS_TYPE = new TypeToken<Map<String, SiteConfig>>() {}.getType();

  private static final Gson gson = new Gson();
  private static final Random random = new Random();
  private static final TagRequestMetricsJsonMarshaller marshaller = new TagRequestMetricsJsonMarshaller(gson);

  private final Map<String, SiteConfig> configs;
  private final Map<String, Double> siteWeights;

  public TagserveLoadGenerator(@Nullable String config) {
    configs = parseSiteConfigs(config);
    siteWeights = parseSiteWeights(configs);
  }

  @Override
  public String nextMessage() {
    return marshaller.marshal(nextMetrics());
  }

  private TagRequestMetrics nextMetrics() {
    String siteId = nextSiteId();
    SiteConfig siteConfig = configs.get(siteId);
    long timestamp = nextTimestamp();
    ImmutableSet<Long> pageIds = nextPages(siteConfig);
    ImmutableSet<Long> tagIds = nextTags(siteConfig, pageIds);
    return new TagRequestMetrics(siteId, timestamp, pageIds, tagIds);
  }

  private long nextTimestamp() {
//    JMeterVariables variables = JMeterContextService.getContext().getVariables();
//    int iteration = variables.getIteration();
    return System.currentTimeMillis();
  }

  private String nextSiteId() {
    // TODO: replace this linear search with binary search
    double r = random.nextDouble();
    for (Map.Entry<String, Double> entry : siteWeights.entrySet()) {
      if (r < entry.getValue()) {
        return entry.getKey();
      }
    }
    // Unreachable if siteWeights is created correctly; checking sum(weights)=1 should prevent this.
    throw new RuntimeException("Unexpected problem randomly selecting a siteId");
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

  private static Map<String, SiteConfig> parseSiteConfigs(String config) {
    try {
      return gson.fromJson(config, SITE_CONFIGS_TYPE);
    } catch (JsonParseException e) {
      log.fatalError("Problem parsing json from config:\n" + config, e);
      throw Throwables.propagate(e);
    }
  }
}