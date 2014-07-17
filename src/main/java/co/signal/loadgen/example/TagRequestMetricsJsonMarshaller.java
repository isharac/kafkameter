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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;

import org.joda.time.Interval;

import com.onetag.metrics.TagRequestMetrics.TagFires;
import com.onetag.metrics.impl.TagFiresImpl;
import com.onetag.metrics.impl.TagRequestMetricsImpl;
import com.onetag.metrics.json.JSONTagRequestMetricsMarshaller;
import com.onetag.model.Source;

/**
 * Example message marshaller which converts a message into JSON.
 *
 * @author codyaray
 * @since 7/17/14
 */
public class TagRequestMetricsJsonMarshaller {
  private final JSONTagRequestMetricsMarshaller delegate = new JSONTagRequestMetricsMarshaller();

  public TagRequestMetricsJsonMarshaller(Gson ignored) {
    // Nothing to do
  }

  public String marshal(TagRequestMetrics metrics) {
    com.onetag.metrics.TagRequestMetrics impl = new TagRequestMetricsImpl(
        metrics.getSiteId(), new Interval(metrics.getTimestamp(), metrics.getTimestamp() + 30),
        Source.WEBSITE, null, false, false, ImmutableSet.copyOf(metrics.getPageIds()),
        toTagFires(metrics.getTagIds()));
    return delegate.marshal(impl);
  }

  public Map<Long, TagFires> toTagFires(Set<Long> tagIds) {
    ImmutableMap.Builder<Long, TagFires> tagMetrics = ImmutableMap.builder();
    for (Long tagId : tagIds) {
      tagMetrics.put(tagId, new TagFiresImpl(1, 0, 0));
    }
    return tagMetrics.build();
  }
}
