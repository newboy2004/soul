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

package org.dromara.soul.plugin.tars;

import org.dromara.soul.common.concurrent.SoulThreadFactory;
import org.dromara.soul.common.constant.Constants;
import org.dromara.soul.common.dto.MetaData;
import org.dromara.soul.common.dto.RuleData;
import org.dromara.soul.common.dto.SelectorData;
import org.dromara.soul.common.enums.PluginEnum;
import org.dromara.soul.common.enums.RpcTypeEnum;
import org.dromara.soul.plugin.api.SoulPluginChain;
import org.dromara.soul.plugin.api.context.SoulContext;
import org.dromara.soul.plugin.api.result.DefaultSoulResult;
import org.dromara.soul.plugin.api.result.SoulResult;
import org.dromara.soul.plugin.api.utils.SpringBeanUtils;
import org.dromara.soul.plugin.tars.cache.ApplicationConfigCache;
import org.dromara.soul.plugin.tars.proxy.TarsInvokePrxList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link TarsPlugin}.
 *
 * @author HoldDie
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class TarsPluginTest {

    @Mock
    private SoulPluginChain chain;

    private MetaData metaData;

    private ServerWebExchange exchange;

    private TarsPlugin tarsPluginUnderTest;

    @Before
    public void setUp() {
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        when(applicationContext.getBean(SoulResult.class)).thenReturn(new DefaultSoulResult());
        SpringBeanUtils springBeanUtils = SpringBeanUtils.getInstance();
        springBeanUtils.setCfgContext(applicationContext);
        metaData = new MetaData("id", "127.0.0.1:8080", "contextPath",
                "path", RpcTypeEnum.TARS.getName(), "serviceName", "method1",
                "parameterTypes", "{\"methodInfo\":[{\"methodName\":\"method1\",\"params\":"
                + "[{\"left\":\"java.lang.String\",\"right\":\"param1\"},{\"left\":\"java.lang.String\","
                + "\"right\":\"param2\"}],\"returnType\":\"java.lang.String\"}]}", false);
        ApplicationConfigCache.getInstance().initPrx(metaData);
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("localhost").build());
        tarsPluginUnderTest = new TarsPlugin();
    }

    @Test
    public void testTarsPluginWithEmptyBody() {
        SoulContext context = mock(SoulContext.class);
        exchange.getAttributes().put(Constants.CONTEXT, context);
        exchange.getAttributes().put(Constants.META_DATA, metaData);
        when(chain.execute(exchange)).thenReturn(Mono.empty());
        RuleData data = mock(RuleData.class);
        SelectorData selectorData = mock(SelectorData.class);
        StepVerifier.create(tarsPluginUnderTest.doExecute(exchange, chain, selectorData, data)).expectSubscription().verifyComplete();
    }

    @Test
    public void testTarsPluginWithEmptyMetaData() {
        SoulContext context = mock(SoulContext.class);
        exchange.getAttributes().put(Constants.CONTEXT, context);
        metaData.setServiceName("");
        exchange.getAttributes().put(Constants.META_DATA, metaData);
        when(chain.execute(exchange)).thenReturn(Mono.empty());
        RuleData data = mock(RuleData.class);
        SelectorData selectorData = mock(SelectorData.class);
        StepVerifier.create(tarsPluginUnderTest.doExecute(exchange, chain, selectorData, data)).expectSubscription().verifyComplete();
    }

    @Test
    public void testTarsPluginWithArgumentTypeMissMatch() {
        SoulContext context = mock(SoulContext.class);
        exchange.getAttributes().put(Constants.CONTEXT, context);
        exchange.getAttributes().put(Constants.META_DATA, metaData);
        exchange.getAttributes().put(Constants.PARAM_TRANSFORM, "{\"param1\":1,\"param2\":2}");
        when(chain.execute(exchange)).thenReturn(Mono.empty());
        RuleData data = mock(RuleData.class);
        SelectorData selectorData = mock(SelectorData.class);
        StepVerifier.create(tarsPluginUnderTest.doExecute(exchange, chain, selectorData, data)).expectSubscription().verifyComplete();
    }

    @Test
    public void testTarsPluginNormal() throws InvocationTargetException, IllegalAccessException {
        SoulContext context = mock(SoulContext.class);
        exchange.getAttributes().put(Constants.CONTEXT, context);
        exchange.getAttributes().put(Constants.META_DATA, metaData);
        exchange.getAttributes().put(Constants.PARAM_TRANSFORM, "{\"param1\":\"1\",\"param2\":\"1\"}");
        when(chain.execute(exchange)).thenReturn(Mono.empty());
        RuleData data = mock(RuleData.class);
        SelectorData selectorData = mock(SelectorData.class);
        TarsInvokePrxList tarsInvokePrxList = ApplicationConfigCache.getInstance().get(metaData.getPath());
        Method method = mock(Method.class);
        ExecutorService executorService = Executors.newFixedThreadPool(1,
                SoulThreadFactory.create("long-polling", true));
        CompletableFuture<String> stringCompletableFuture = CompletableFuture.supplyAsync(() -> "", executorService);
        when(method.invoke(any(), any())).thenReturn(stringCompletableFuture);
        tarsInvokePrxList.setMethod(method);
        StepVerifier.create(tarsPluginUnderTest.doExecute(exchange, chain, selectorData, data)).expectSubscription().verifyComplete();
    }

    @Test
    public void testGetOrder() {
        int result = tarsPluginUnderTest.getOrder();
        assertEquals(PluginEnum.TARS.getCode(), result);
    }

    @Test
    public void testNamed() {
        String result = tarsPluginUnderTest.named();
        assertEquals(PluginEnum.TARS.getName(), result);
    }

    @Test
    public void testSkip() {
        SoulContext context = mock(SoulContext.class);
        when(context.getRpcType()).thenReturn(RpcTypeEnum.TARS.getName());
        exchange.getAttributes().put(Constants.CONTEXT, context);
        Boolean result = tarsPluginUnderTest.skip(exchange);
        assertFalse(result);
    }
}
