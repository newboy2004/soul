/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.soul.plugin.apache.dubbo;

import org.dromara.soul.common.constant.Constants;
import org.dromara.soul.common.dto.MetaData;
import org.dromara.soul.common.dto.RuleData;
import org.dromara.soul.common.dto.SelectorData;
import org.dromara.soul.common.enums.PluginEnum;
import org.dromara.soul.common.enums.RpcTypeEnum;
import org.dromara.soul.plugin.apache.dubbo.proxy.ApacheDubboProxyService;
import org.dromara.soul.plugin.api.SoulPluginChain;
import org.dromara.soul.plugin.api.context.SoulContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The Test Case For ApachDubboPlugin.
 *
 * @author nuo-promise
 **/
@RunWith(MockitoJUnitRunner.class)
public final class ApacheDubboPluginTest {

    private ApacheDubboPlugin apacheDubboPlugin;

    private MetaData metaData;

    private ServerWebExchange exchange;

    @Mock
    private SoulPluginChain chain;

    @Before
    public void setUp() {
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("localhost").build());
        metaData = new MetaData();
        metaData.setId("1332017966661636096");
        metaData.setAppName("dubbo");
        metaData.setPath("/dubbo/findAll");
        metaData.setServiceName("org.dromara.soul.test.dubbo.api.service.DubboTestService");
        metaData.setMethodName("findAll");
        metaData.setRpcType(RpcTypeEnum.DUBBO.getName());
        ApacheDubboProxyService apacheDubboProxyService = mock(ApacheDubboProxyService.class);
        apacheDubboPlugin = new ApacheDubboPlugin(apacheDubboProxyService);
    }

    @Test(expected = NullPointerException.class)
    public void doExecute() {
        SoulContext context = mock(SoulContext.class);
        exchange.getAttributes().put(Constants.CONTEXT, context);
        exchange.getAttributes().put(Constants.PARAM_TRANSFORM, "{key:value}");
        exchange.getAttributes().put(Constants.META_DATA, metaData);
        when(chain.execute(exchange)).thenReturn(Mono.empty());
        SelectorData selectorData = mock(SelectorData.class);
        RuleData data = mock(RuleData.class);
        StepVerifier.create(apacheDubboPlugin.doExecute(exchange, chain, selectorData, data)).expectSubscription().verifyComplete();
    }

    @Test(expected = NullPointerException.class)
    public void testParameterNotNullExecute() {
        SoulContext context = mock(SoulContext.class);
        exchange.getAttributes().put(Constants.CONTEXT, context);
        metaData.setParameterTypes("parameterTypes");
        exchange.getAttributes().put(Constants.META_DATA, metaData);
        SelectorData selectorData = mock(SelectorData.class);
        RuleData data = mock(RuleData.class);
        StepVerifier.create(apacheDubboPlugin.doExecute(exchange, chain, selectorData, data)).expectSubscription().verifyComplete();
    }

    @Test(expected = NullPointerException.class)
    public void testMethodIsNullExecute() {
        SoulContext context = mock(SoulContext.class);
        exchange.getAttributes().put(Constants.CONTEXT, context);
        exchange.getAttributes().put(Constants.PARAM_TRANSFORM, "{key:value}");
        MetaData metaData = MetaData.builder()
                .id("1332017966661636096")
                .appName("dubbo")
                .path("/dubbo/findAll")
                .serviceName("org.dromara.soul.test.dubbo.api.service.DubboTestService")
                .rpcType(RpcTypeEnum.DUBBO.getName())
                .build();
        exchange.getAttributes().put(Constants.META_DATA, metaData);
        SelectorData selectorData = mock(SelectorData.class);
        RuleData data = mock(RuleData.class);
        StepVerifier.create(apacheDubboPlugin.doExecute(exchange, chain, selectorData, data)).expectSubscription().verifyComplete();
    }

    @Test
    public void testNamed() {
        final String result = apacheDubboPlugin.named();
        assertEquals(PluginEnum.DUBBO.getName(), result);
    }

    @Test
    public void skip() {
        final ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("localhost").build());
        SoulContext context = mock(SoulContext.class);
        when(context.getRpcType()).thenReturn(RpcTypeEnum.DUBBO.getName());
        exchange.getAttributes().put(Constants.CONTEXT, context);
        exchange.getAttributes().put(Constants.META_DATA, metaData);
        final Boolean result = apacheDubboPlugin.skip(exchange);
        assertFalse(result);
    }

    @Test
    public void getOrder() {
        final int result = apacheDubboPlugin.getOrder();
        assertEquals(PluginEnum.DUBBO.getCode(), result);
    }
}
