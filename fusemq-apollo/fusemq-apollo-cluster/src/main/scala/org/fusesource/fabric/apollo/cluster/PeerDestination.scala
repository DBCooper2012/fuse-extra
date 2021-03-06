/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.cluster

import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.broker._
import org.fusesource.fabric.apollo.cluster.protocol.ClusterProtocolHandler
import org.fusesource.fabric.apollo.cluster.model._
import org.fusesource.hawtbuf.Buffer._
import org.fusesource.hawtbuf.ByteArrayOutputStream
import org.apache.activemq.apollo.dto._


/**
 * Channels can switch between connections
 */
class PeerDestination(val local_destination:DomainDestination, val peer:Peer) extends DomainDestination {

  /////////////////////////////////////////////////////////////////////////////
  // DomainDestination interface
  /////////////////////////////////////////////////////////////////////////////
  def virtual_host = local_destination.virtual_host
  def address = local_destination.address

  // setup the local destination to forward to the peer..
  local_destination.bind(local_destination.address.asInstanceOf[BindAddress], forwarding_consumer)

  def close() = {
    // stop forwarding...
    local_destination.unbind(forwarding_consumer, false)
  }

  def connect(destination: ConnectAddress, producer: BindableDeliveryProducer) = {
    producer.bind(forwarding_consumer::Nil)
  }

  def disconnect(producer: BindableDeliveryProducer) = {
    producer.unbind(forwarding_consumer::Nil)
  }

  def bind(address: BindAddress, consumer: DeliveryConsumer) = {

    val destination_dto = address.domain match {
      case "queue" => new QueueDestinationDTO(address.id);
      case "topic" => new TopicDestinationDTO(address.id);
      case "dsub" => 
        val rc = new DurableSubscriptionDestinationDTO(address.id);
        address match {
          case address:SubscriptionAddress => 
            rc.selector = address.selector
            address.topics.foreach { topic =>
              rc.topics.add( new TopicDestinationDTO(address.id));
            }
            rc
          case address =>
            rc.direct()
        }  
        rc
    }
    
    val os = new ByteArrayOutputStream()
    XmlCodec.encode(destination_dto, os, false)

    val bean = new ConsumerInfo.Bean
    bean.setVirtualHost(ascii(virtual_host.config.host_names.get(0)))
    bean.addDestination(os.toBuffer)

    val exported = peer.add_cluster_consumer(bean, consumer)
  }

  def unbind(consumer: DeliveryConsumer, persistent: Boolean) = {
  }

  /////////////////////////////////////////////////////////////////////////////
  // DeliveryConsumer that forwards messages to the peer
  /////////////////////////////////////////////////////////////////////////////
  object forwarding_consumer extends BaseRetained with DeliveryConsumer {

    def dispatch_queue = peer.dispatch_queue

    var handler:ClusterProtocolHandler = _

    override def connection = None
    def matches(message: Delivery): Boolean = true
    def is_persistent: Boolean = false

    def connect(p: DeliveryProducer): DeliverySession = {

      val destination_dto = address.domain match {
        case "queue" => new QueueDestinationDTO(address.id);
        case "topic" => new TopicDestinationDTO(address.id);
        case "dsub" =>
          val rc = new DurableSubscriptionDestinationDTO(address.id);
          address match {
            case address:SubscriptionAddress =>
              rc.selector = address.selector
              address.topics.foreach { topic =>
                rc.topics.add( new TopicDestinationDTO(address.id));
              }
              rc
            case address =>
              rc.direct()
          }
          rc
      }

      val os = new ByteArrayOutputStream()
      XmlCodec.encode(destination_dto, os, false)
      val bean = new ChannelOpen.Bean
      bean.setVirtualHost(ascii(virtual_host.config.host_names.get(0)))
      bean.addDestination(os.toBuffer)
      bean

      new MutableSink[Delivery] with DeliverySession {

        var closed = false
        val channel = peer.open_channel(p.dispatch_queue, bean)
        if( !closed ) {
          downstream = Some(channel)
        } else {
          channel.close
        }

        def producer: DeliveryProducer = p
        def consumer: DeliveryConsumer = forwarding_consumer

        def close: Unit = {
          if( !closed ) {
            closed = true
            downstream.foreach(_.asInstanceOf[Peer#OutboundChannelSink].close)
          }
        }

        def remaining_capacity = downstream.map(_.asInstanceOf[Peer#OutboundChannelSink].remaining_capacity).getOrElse(0)

        @volatile
        var enqueue_item_counter = 0L
        @volatile
        var enqueue_size_counter = 0L
        @volatile
        var enqueue_ts = 0L

        override def offer(value: Delivery) = {
          if( super.offer(value) ){
            enqueue_item_counter += 1
            enqueue_size_counter += value.size
            enqueue_ts = now
            true
          } else {
            false
          }
        }

      }

    }

    override def toString = "cluster %s on node %d".format(address, peer.id)
  }

  def now = virtual_host.broker.now

  def update(on_completed: Task) = local_destination.update(on_completed)

  def resource_kind = local_destination.resource_kind
}


