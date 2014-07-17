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

import java.util.Set;

/**
 * Example model object representing a single domain-specific message.
 *
 * @author codyaray
 * @since 7/17/14
 */
public class TagRequestMetrics {
  private final String siteId;
  private final long timestamp;
  private final Set<Long> pageIds;
  private final Set<Long> tagIds;

  public TagRequestMetrics(String siteId, long timestamp, Set<Long> pageIds, Set<Long> tagIds) {
    this.siteId = siteId;
    this.timestamp = timestamp;
    this.pageIds = pageIds;
    this.tagIds = tagIds;
  }

  public String getSiteId() {
    return siteId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Set<Long> getPageIds() {
    return pageIds;
  }

  public Set<Long> getTagIds() {
    return tagIds;
  }
}
