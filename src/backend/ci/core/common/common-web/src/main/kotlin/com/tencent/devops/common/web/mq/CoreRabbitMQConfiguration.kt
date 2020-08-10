/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.web.mq

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.web.mq.property.CoreRabbitMQProperties
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary


@Configuration
@EnableConfigurationProperties(CoreRabbitMQProperties::class)
class CoreRabbitMQConfiguration {

    @Value("\${spring.rabbitmq.core.virtual-host}")
    private val virtualHost: String? = null
    @Value("\${spring.rabbitmq.core.username}")
    private val username: String? = null
    @Value("\${spring.rabbitmq.core.password}")
    private val password: String? = null
    @Value("\${spring.rabbitmq.core.addresses}")
    private val addresses: String? = null
    @Value("\${spring.rabbitmq.core.listener.simple.concurrency}")
    private val concurrency: Int? = null
    @Value("\${spring.rabbitmq.core.listener.simple.max-concurrency}")
    private val maxConcurrency: Int? = null

    @Bean(name = [CORE_CONNECTION_FACTORY_NAME])
    @Primary
    fun coreConnectionFactory(config: CoreRabbitMQProperties): ConnectionFactory {
        val connectionFactory = CachingConnectionFactory()
        connectionFactory.host = config.host
        connectionFactory.port = config.port
        connectionFactory.username = username
        connectionFactory.setPassword(password)
        connectionFactory.virtualHost = virtualHost
        connectionFactory.setAddresses(addresses)
        return connectionFactory
    }

    @Bean(name = [CORE_RABBIT_TEMPLATE_NAME])
    @Primary
    fun coreRabbitTemplate(
        @Qualifier(CORE_CONNECTION_FACTORY_NAME)
        connectionFactory: ConnectionFactory,
        objectMapper: ObjectMapper
    ): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = messageConverter(objectMapper)
        return rabbitTemplate
    }

    @Bean(value = [CORE_RABBIT_ADMIN_NAME])
    @Primary
    fun coreRabbitAdmin(
        @Qualifier(CORE_CONNECTION_FACTORY_NAME)
        connectionFactory: ConnectionFactory
    ): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean(value = [CORE_FACTORY_NAME])
    fun coreFactory(
        @Qualifier(CORE_CONNECTION_FACTORY_NAME)
        connectionFactory: ConnectionFactory
    ): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()
        factory.setConnectionFactory(connectionFactory)
        if (concurrency != null) {
            factory.setConcurrentConsumers(concurrency)
        }
        if (maxConcurrency != null) {
            factory.setMaxConcurrentConsumers(maxConcurrency)
        }

        return factory
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) =
        Jackson2JsonMessageConverter(objectMapper)
}