# kafkameter - Kafka JMeter Plugin

This plugin provides two components:

* Kafka Producer Sampler: sends keyed messages to Kafka
* Tagserve Load Generator Config Element: generates JSON TagRequestMetrics messages as synthetic load from tagserve.

The purpose of the Load Generator Config Element is to dynamically create the messages for sending
to Kafka. You could alternatively use the CSV Data Set or another source for messages.

The included Tagserve Load Generator is intended for use as an example; your message format
and load distribution will undoubtedly differ from ours.

## Install

Build the plugin:

    mvn package

Install the plugin into `$JMETER_HOME/ext/lib`:

    cp target/kafkameter-x.y.z.jar $JMETER_HOME/ext/lib

## Usage

### Kafka Producer Sampler

After installing the plugin, add a Java Request Sampler and select the `KafkaProducerSampler`
class name. The following properties are required.

* **kafka_brokers**: comma-separated list of hosts in the format (hostname:port).
* **kafka_topic**: the topic in Kafka to which the message will be published.
* **kafka_key**: the partition key for the message.
* **kafka_message**: the message itself.

You may also override the following:

* **kafka_message_serializer**: the Kafka client `serializer.class` property.
* **kafka_key_serializer**: the Kafka client `key.serializer.class` property.

### Tagserve Load Generator

After installing the plugin, the Tagserve Load Generator will be available as a Config Element.
This component reads a Synthetic Tagserve Load Description from a given file, generates a JSON
TagRequestMetrics message, and makes it available with the given variable name.

#### Synthetic Tagserve Load Description

The Synthetic Tagserve Load Description has the following format:

    {
      <siteId>: {
        "weight": <double>,
        "pages": {
            <pageId>: {
                "weight": <double>,
                "tags": [<tagId>, ...]
            }
        }
      }
    }

The weights are used to determine the next `TagRequestMetrics` message for the `KafkaProducerSampler`.
The weight for a site represents the percentage of total traffic belonging to the site. However,
the weight for a page represents the probability that the page will be matched in any given request.

Said another way, because a `TagRequestMetric` represents a single site, the weights for the different
sites must sum to unity. However, a single request can match multiple pages independently, so these
weights are independent; i.e., they do not have to sum to unity.

#### Synthetic Tagserve Load Algorithm

For each iteration, generate a uniformly random variate between `(0, 1]`. The mapping below would
represent which site's load configuration to use based on the variate.

    site1: (0.0, 0.6]
    site2: (0.6, 1.0]

For each page within the load configuration, generate a uniformly random variate between `(0, 1]`.
Reject any pages with lesser weights. The tags included from each selected page are unioned.

#### Example

For example, given the following synthetic tagserve load configuration

    {
       "site1": {
          "weight": 0.6,
          "pages": {
              "123": {
                 "weight": 0.7,
                 "tags": [123, 567]
              },
              "234": {
                  "weight": 0.1,
                  "tags": [123, 234, 345, 456]
              }
          },
       },
       "site2": {
          "weight": 0.4,
          "pages": {
              "123": {
                 "weight": 0.7,
                 "tags": [123, 234]
              }
          }
       }
    }

if the random variates were (0.4, 0.6, 0.05), then we would match the load config for
"site1" with pages [123, 234] and tags [123, 234, 345, 456, 567].
