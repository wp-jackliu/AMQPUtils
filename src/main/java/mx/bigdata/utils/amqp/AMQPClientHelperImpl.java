/*
 *  Copyright 2010 BigData Mx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package mx.bigdata.utils.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import mx.bigdata.anyobject.AnyObject;

public final class AMQPClientHelperImpl implements AMQPClientHelper {
  
  private static final int DEFAULT_QOS = 1000;

  private static final String DEFAULT_ROUTING_KEY = "-";

  private final AnyObject conf;

  public AMQPClientHelperImpl(AnyObject conf) {
    this.conf = conf;
  }

  public ConnectionFactory createConnectionFactory() throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    String username = conf.getString("cf.username");
    if (username != null) {
      factory.setUsername(username);
    }
    String password = conf.getString("cf.password");
    if (password != null) {
      factory.setPassword(password);
    }
    String virtualHost = conf.getString("cf.virtual_host");
    if ( virtualHost != null) {
      factory.setVirtualHost(virtualHost);
    }
    String host = conf.getString("cf.host");
    if (host != null) {
      factory.setHost(host);
    }
    Integer port = conf.getInteger("cf.port");
    if (port != null) {
      factory.setPort(port);
    }
    return factory;
  }
  
  public Channel declareChannel(ConnectionFactory factory, 
                                   String key) throws Exception {
    Connection conn = factory.newConnection();
    Channel channel = conn.createChannel();
    Integer basicQos = conf.getInteger("channel.basic_qos");
    if (basicQos != null) {
      channel.basicQos(basicQos);
    } else {
      channel.basicQos(DEFAULT_QOS);
    }
    channel.exchangeDeclare(getExchangeName(key), 
                            getExchangeType(key), 
                            true);
    return channel;
  }

  public String createQueue(Channel channel, String key) throws Exception {
    AMQP.Queue.DeclareOk result = channel.queueDeclare();
    String queue = result.getQueue();
    channel.queueBind(queue, getExchangeName(key), getRoutingKey(key));
    return queue;
  }

  public String getRoutingKey() {
    return getRoutingKey(null);
  }
  public String getRoutingKey(String key) {
    String rk = conf.getString("queue.routing_key"
                               + ((key != null) ? "." +  key : ""));
    return (rk != null) ? rk : DEFAULT_ROUTING_KEY; 
  }

  public String getExchangeName(String key) {
    return conf.getString("exchange.name"
                          + ((key != null) ? "." +  key : ""));
  }

  public String getExchangeType(String key) {
    return conf.getString("exchange.type" 
                          + ((key != null) ? "." +  key : ""));
  }

  public QueueingConsumer createQueueingConsumer(Channel channel, 
                                                    String queue) 
    throws Exception {
    QueueingConsumer consumer = new QueueingConsumer(channel);
    channel.basicConsume(queue, false, consumer);
    return consumer;
  }

  public byte[] getBodyAndAck(Channel channel, QueueingConsumer consumer) 
    throws Exception {
      QueueingConsumer.Delivery delivery = consumer.nextDelivery();  
      byte[] body = delivery.getBody();   
      long deliveryTag = delivery.getEnvelope().getDeliveryTag();       
      channel.basicAck(deliveryTag, true);
      return body;
  }

  public void ack(Channel channel, QueueingConsumer.Delivery delivery) 
    throws Exception {
      long deliveryTag = delivery.getEnvelope().getDeliveryTag();       
      channel.basicAck(deliveryTag, true);
  }

  public void reject(Channel channel, QueueingConsumer.Delivery delivery) 
    throws Exception {
      long deliveryTag = delivery.getEnvelope().getDeliveryTag();       
      channel.basicReject(deliveryTag, true);
  }
}