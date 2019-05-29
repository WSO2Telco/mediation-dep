package com.wso2telco.dep.mediator.impl.payment;

/**
 * Copyright (c) 2019, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 * <p>
 * WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.delegator.CarbonUtilsDelegator;
import com.wso2telco.dep.mediator.delegator.OCCIDelegator;
import com.wso2telco.dep.mediator.delegator.ValidatorUtilsDelegator;
import com.wso2telco.dep.mediator.entity.OparatorEndPointSearchDTO;
import com.wso2telco.dep.mediator.internal.ApiUtils;
import com.wso2telco.dep.mediator.service.PaymentService;
import com.wso2telco.dep.subscriptionvalidator.services.MifeValidator;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;

public class AmountChargeHandlerTest {

    @Test
    public void testNullOrTrimmed_WhenStringIsNull() {
        String s = AmountChargeHandler.nullOrTrimmed(null);
        Assert.assertEquals(s, null);
    }


    @Test
    public void amountChargeHandler_test() throws Exception {
        MessageContext mockMessageContext = Mockito.mock(MessageContext.class);
        Axis2MessageContext mockAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
        Mockito.when(mockAxis2MessageContext.getAxis2MessageContext()).thenReturn(mockMessageContext);
        HashMap headerMap = new HashMap<>();
        headerMap.put("applicationid", "69");
        headerMap.put("subscriber", "admin");
        headerMap.put("API_ID", "5");
        headerMap.put("x-jwt-assertion", "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6ImFfamhOdXMyMUtWdW9GeDY1TG1rVzJPX2wxMCJ9.eyJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9hcHBsaWNhdGlvbnRpZXIiOiJEZWZhdWx0IiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wva2V5dHlwZSI6IlBST0RVQ1RJT04iLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC92ZXJzaW9uIjoidjEiLCJpc3MiOiJ3c28yLm9yZ1wvcHJvZHVjdHNcL2FtIiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBwbGljYXRpb25uYW1lIjoiVGVzdCIsImh0dHA6XC9cL3dzbzIub3JnXC9jbGFpbXNcL2VuZHVzZXIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9lbmR1c2VyVGVuYW50SWQiOiItMTIzNCIsImh0dHA6XC9cL3dzbzIub3JnXC9jbGFpbXNcL3N1YnNjcmliZXIiOiJhZG1pbiIsImh0dHA6XC9cL3dzbzIub3JnXC9jbGFpbXNcL3RpZXIiOiJEZWZhdWx0IiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBwbGljYXRpb25pZCI6IjY5IiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvdXNlcnR5cGUiOiJBUFBMSUNBVElPTiIsImV4cCI6MTU1ODQ5ODk5NiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBpY29udGV4dCI6IlwvcGF5bWVudFwvdjEifQ==.CP+4q0zjhVY0xQedNv48jCZF0qRoMmSw1OinHeyq+7UOyEqvh0r2e/wfW2dDlWg1So/erd2eyrPBPVK+VwruVDM2q0IqdhFvo4ef2GsbvjL");
        Mockito.when(mockMessageContext.getProperty(MessageContext.TRANSPORT_HEADERS)).thenReturn(headerMap);
        Mockito.when(mockAxis2MessageContext.getProperty("APPLICATION_ID")).thenReturn("69");
        Mockito.when(mockAxis2MessageContext.getProperty("API_ID")).thenReturn("69");
        Mockito.when(mockAxis2MessageContext.getProperty("MSISDN")).thenReturn("tel:+94777323228");
        Mockito.when(mockAxis2MessageContext.getProperty("API_NAME")).thenReturn("payment");

        CarbonUtilsDelegator mockCarbonUtilsDelegator = Mockito.mock(CarbonUtilsDelegator.class);
        Mockito.when(mockCarbonUtilsDelegator.getCarbonConfigDirPath()).thenReturn("");
        PaymentExecutor mockPaymentExecutor = Mockito.mock(PaymentExecutor.class);
        OCCIDelegator mockOcci = Mockito.mock(OCCIDelegator.class);
        PaymentService mockPaymentService = Mockito.mock(PaymentService.class);
        ApiUtils mockApiUtils = Mockito.mock(ApiUtils.class);
        PaymentUtil mockPaymentUtil = Mockito.mock(PaymentUtil.class);
        FileReader mockFileReader = Mockito.mock(FileReader.class);

        Mockito.when(mockApiUtils.getJwtTokenDetails(mockAxis2MessageContext)).thenReturn(headerMap);

        Map<String, String> mediatorConfMap = new HashMap<>();
        mediatorConfMap.put("hub_gateway_id", "0001");
        mediatorConfMap.put("hubGateway", "https://gateway1a.mife.sla-mobile.com.my:8243");
        Mockito.when(mockFileReader.readPropertyFile("/mediator-conf.properties")).thenReturn(mediatorConfMap);

        Mockito.when(mockPaymentExecutor.getResourceUrl()).thenReturn("/payment/94777323228/transactions/amount");
        JSONObject jsonObject = new JSONObject("{\"amountTransaction\":{\"endUserId\":\"tel:+SBcRDj/+M108gFCu1S56zw==\"," +
                "\"transactionOperationStatus\":\"Charged\",\"clientCorrelator\":\"TES35cctrd25\",\"referenceCode\":\"REF-TEce2dfdwe\"," +
                "\"paymentAmount\":{\"chargingInformation\":{\"amount\":\"100\",\"description\":\"Alien Invaders Game\"," +
                "\"currency\":\"USD\"},\"chargingMetaData\":{\"channel\":\"sms\",\"onBehalfOf\":\"Merchant\",\"taxAmount\":\"0\"}}}}\n");
        Mockito.when(mockPaymentExecutor.getJsonBody()).thenReturn(jsonObject);

        ValidatorUtilsDelegator mockValidatorUtilsDelegator = Mockito.mock(ValidatorUtilsDelegator.class);
        MifeValidator validator = Mockito.mock(MifeValidator.class);
        Mockito.when(mockValidatorUtilsDelegator.getValidatorForSubscriptionFromMessageContext(mockAxis2MessageContext)).thenReturn(validator);
        Mockito.when(validator.validate(mockAxis2MessageContext)).thenReturn(true);

        OperatorEndpoint mockEndpoint = Mockito.mock(OperatorEndpoint.class);
        EndpointReference mockEndpointReference = Mockito.mock(EndpointReference.class);
        Mockito.when(mockOcci.getOperatorEndpoint((OparatorEndPointSearchDTO) any())).thenReturn(mockEndpoint);

        Mockito.when(mockEndpoint.getEndpointref()).thenReturn(mockEndpointReference);
        Mockito.when(mockEndpointReference.getAddress()).thenReturn("http://10.7.0.103:8280/payment/SBcRDj%2F%2BM108gFCu1S56zw%3D%3D/transactions/amount");

        AmountChargeHandler amountChargeHandler = new AmountChargeHandler(mockOcci, mockPaymentService, mockPaymentExecutor,
                mockApiUtils, mockPaymentUtil, mockCarbonUtilsDelegator, mockFileReader, mockValidatorUtilsDelegator);
        boolean result = amountChargeHandler.handle(mockAxis2MessageContext);
        Assert.assertEquals(result, true);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void amountChargeHandler_whenOparatorEndPointSearchDTOisNull() throws Exception {
        MessageContext mockMessageContext = Mockito.mock(MessageContext.class);
        Axis2MessageContext mockAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
        Mockito.when(mockAxis2MessageContext.getAxis2MessageContext()).thenReturn(mockMessageContext);
        HashMap headerMap = new HashMap<>();
        headerMap.put("applicationid", "69");
        headerMap.put("subscriber", "admin");
        headerMap.put("API_ID", "5");
        headerMap.put("x-jwt-assertion", "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6ImFfamhOdXMyMUtWdW9GeDY1TG1rVzJPX2wxMCJ9.eyJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9hcHBsaWNhdGlvbnRpZXIiOiJEZWZhdWx0IiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wva2V5dHlwZSI6IlBST0RVQ1RJT04iLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC92ZXJzaW9uIjoidjEiLCJpc3MiOiJ3c28yLm9yZ1wvcHJvZHVjdHNcL2FtIiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBwbGljYXRpb25uYW1lIjoiVGVzdCIsImh0dHA6XC9cL3dzbzIub3JnXC9jbGFpbXNcL2VuZHVzZXIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9lbmR1c2VyVGVuYW50SWQiOiItMTIzNCIsImh0dHA6XC9cL3dzbzIub3JnXC9jbGFpbXNcL3N1YnNjcmliZXIiOiJhZG1pbiIsImh0dHA6XC9cL3dzbzIub3JnXC9jbGFpbXNcL3RpZXIiOiJEZWZhdWx0IiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBwbGljYXRpb25pZCI6IjY5IiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvdXNlcnR5cGUiOiJBUFBMSUNBVElPTiIsImV4cCI6MTU1ODQ5ODk5NiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBpY29udGV4dCI6IlwvcGF5bWVudFwvdjEifQ==.CP+4q0zjhVY0xQedNv48jCZF0qRoMmSw1OinHeyq+7UOyEqvh0r2e/wfW2dDlWg1So/erd2eyrPBPVK+VwruVDM2q0IqdhFvo4ef2GsbvjL");
        Mockito.when(mockMessageContext.getProperty(MessageContext.TRANSPORT_HEADERS)).thenReturn(headerMap);
        Mockito.when(mockAxis2MessageContext.getProperty("APPLICATION_ID")).thenReturn("69");
        Mockito.when(mockAxis2MessageContext.getProperty("API_ID")).thenReturn("69");
        Mockito.when(mockAxis2MessageContext.getProperty("MSISDN")).thenReturn("tel:+94777323228");
        Mockito.when(mockAxis2MessageContext.getProperty("API_NAME")).thenReturn("payment");

        CarbonUtilsDelegator mockCarbonUtilsDelegator = Mockito.mock(CarbonUtilsDelegator.class);
        Mockito.when(mockCarbonUtilsDelegator.getCarbonConfigDirPath()).thenReturn("");
        PaymentExecutor mockPaymentExecutor = Mockito.mock(PaymentExecutor.class);
        OCCIDelegator mockOcci = Mockito.mock(OCCIDelegator.class);
        PaymentService mockPaymentService = Mockito.mock(PaymentService.class);
        ApiUtils mockApiUtils = Mockito.mock(ApiUtils.class);
        PaymentUtil mockPaymentUtil = Mockito.mock(PaymentUtil.class);
        FileReader mockFileReader = Mockito.mock(FileReader.class);

        Mockito.when(mockApiUtils.getJwtTokenDetails(mockAxis2MessageContext)).thenReturn(headerMap);

        Map<String, String> mediatorConfMap = new HashMap<>();
        mediatorConfMap.put("hub_gateway_id", "0001");
        mediatorConfMap.put("hubGateway", "https://gateway1a.mife.sla-mobile.com.my:8243");
        Mockito.when(mockFileReader.readPropertyFile("/mediator-conf.properties")).thenReturn(mediatorConfMap);

        Mockito.when(mockPaymentExecutor.getResourceUrl()).thenReturn("/payment/94777323228/transactions/amount");
        JSONObject jsonObject = new JSONObject("{\"amountTransaction\":{\"endUserId\":\"tel:+SBcRDj/+M108gFCu1S56zw==\"," +
                "\"transactionOperationStatus\":\"Charged\",\"clientCorrelator\":\"TES35cctrd25\",\"referenceCode\":\"REF-TEce2dfdwe\"," +
                "\"paymentAmount\":{\"chargingInformation\":{\"amount\":\"100\",\"description\":\"Alien Invaders Game\"," +
                "\"currency\":\"USD\"},\"chargingMetaData\":{\"channel\":\"sms\",\"onBehalfOf\":\"Merchant\",\"taxAmount\":\"0\"}}}}\n");
        Mockito.when(mockPaymentExecutor.getJsonBody()).thenReturn(jsonObject);

        ValidatorUtilsDelegator mockValidatorUtilsDelegator = Mockito.mock(ValidatorUtilsDelegator.class);
        MifeValidator validator = Mockito.mock(MifeValidator.class);
        Mockito.when(mockValidatorUtilsDelegator.getValidatorForSubscriptionFromMessageContext(mockAxis2MessageContext)).thenReturn(validator);
        Mockito.when(validator.validate(mockAxis2MessageContext)).thenReturn(true);

        OperatorEndpoint mockEndpoint = Mockito.mock(OperatorEndpoint.class);
        EndpointReference mockEndpointReference = Mockito.mock(EndpointReference.class);
        Mockito.when(mockOcci.getOperatorEndpoint( null)).thenReturn(mockEndpoint);

        Mockito.when(mockEndpoint.getEndpointref()).thenReturn(mockEndpointReference);
        Mockito.when(mockEndpointReference.getAddress()).thenReturn("http://10.7.0.103:8280/payment/SBcRDj%2F%2BM108gFCu1S56zw%3D%3D/transactions/amount");

        AmountChargeHandler amountChargeHandler = new AmountChargeHandler(mockOcci, mockPaymentService, mockPaymentExecutor,
                mockApiUtils, mockPaymentUtil, mockCarbonUtilsDelegator, mockFileReader, mockValidatorUtilsDelegator);
        boolean result = amountChargeHandler.handle(mockAxis2MessageContext);
        Assert.assertEquals(result, true);
    }

    @Test
    public void amountChargeHandler_whenUserAnonymization() throws Exception {
        MessageContext mockMessageContext = Mockito.mock(MessageContext.class);
        Axis2MessageContext mockAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
        Mockito.when(mockAxis2MessageContext.getAxis2MessageContext()).thenReturn(mockMessageContext);
        HashMap headerMap = new HashMap<>();
        headerMap.put("applicationid", "69");
        headerMap.put("subscriber", "admin");
        headerMap.put("API_ID", "5");
        headerMap.put("x-jwt-assertion", "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6ImFfamhOdXMyMUtWdW9GeDY1TG1rVzJPX2wxMCJ9.eyJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9hcHBsaWNhdGlvbnRpZXIiOiJEZWZhdWx0IiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wva2V5dHlwZSI6IlBST0RVQ1RJT04iLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC92ZXJzaW9uIjoidjEiLCJpc3MiOiJ3c28yLm9yZ1wvcHJvZHVjdHNcL2FtIiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBwbGljYXRpb25uYW1lIjoiVGVzdCIsImh0dHA6XC9cL3dzbzIub3JnXC9jbGFpbXNcL2VuZHVzZXIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9lbmR1c2VyVGVuYW50SWQiOiItMTIzNCIsImh0dHA6XC9cL3dzbzIub3JnXC9jbGFpbXNcL3N1YnNjcmliZXIiOiJhZG1pbiIsImh0dHA6XC9cL3dzbzIub3JnXC9jbGFpbXNcL3RpZXIiOiJEZWZhdWx0IiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBwbGljYXRpb25pZCI6IjY5IiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvdXNlcnR5cGUiOiJBUFBMSUNBVElPTiIsImV4cCI6MTU1ODQ5ODk5NiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBpY29udGV4dCI6IlwvcGF5bWVudFwvdjEifQ==.CP+4q0zjhVY0xQedNv48jCZF0qRoMmSw1OinHeyq+7UOyEqvh0r2e/wfW2dDlWg1So/erd2eyrPBPVK+VwruVDM2q0IqdhFvo4ef2GsbvjL");
        Mockito.when(mockMessageContext.getProperty(MessageContext.TRANSPORT_HEADERS)).thenReturn(headerMap);
        Mockito.when(mockAxis2MessageContext.getProperty("APPLICATION_ID")).thenReturn("69");
        Mockito.when(mockAxis2MessageContext.getProperty("API_ID")).thenReturn("69");
        Mockito.when(mockAxis2MessageContext.getProperty("MSISDN")).thenReturn("tel:+94777323228");
        Mockito.when(mockAxis2MessageContext.getProperty("API_NAME")).thenReturn("payment");

        CarbonUtilsDelegator mockCarbonUtilsDelegator = Mockito.mock(CarbonUtilsDelegator.class);
        Mockito.when(mockCarbonUtilsDelegator.getCarbonConfigDirPath()).thenReturn("");
        PaymentExecutor mockPaymentExecutor = Mockito.mock(PaymentExecutor.class);
        OCCIDelegator mockOcci = Mockito.mock(OCCIDelegator.class);
        PaymentService mockPaymentService = Mockito.mock(PaymentService.class);
        ApiUtils mockApiUtils = Mockito.mock(ApiUtils.class);
        PaymentUtil mockPaymentUtil = Mockito.mock(PaymentUtil.class);
        FileReader mockFileReader = Mockito.mock(FileReader.class);

        Mockito.when(mockApiUtils.getJwtTokenDetails(mockAxis2MessageContext)).thenReturn(headerMap);

        Map<String, String> mediatorConfMap = new HashMap<>();
        mediatorConfMap.put("hub_gateway_id", "0001");
        mediatorConfMap.put("hubGateway", "https://gateway1a.mife.sla-mobile.com.my:8243");
        Mockito.when(mockFileReader.readPropertyFile("/mediator-conf.properties")).thenReturn(mediatorConfMap);

        Mockito.when(mockPaymentExecutor.getResourceUrl()).thenReturn("/payment/94777323228/transactions/amount");
        JSONObject jsonObject = new JSONObject("{\"amountTransaction\":{\"endUserId\":\"tel:+SBcRDj/+M108gFCu1S56zw==\"," +
                "\"transactionOperationStatus\":\"Charged\",\"clientCorrelator\":\"TES35cctrd25\",\"referenceCode\":\"REF-TEce2dfdwe\"," +
                "\"paymentAmount\":{\"chargingInformation\":{\"amount\":\"100\",\"description\":\"Alien Invaders Game\"," +
                "\"currency\":\"USD\"},\"chargingMetaData\":{\"channel\":\"sms\",\"onBehalfOf\":\"Merchant\",\"taxAmount\":\"0\"}}}}\n");
        Mockito.when(mockPaymentExecutor.getJsonBody()).thenReturn(jsonObject);

        ValidatorUtilsDelegator mockValidatorUtilsDelegator = Mockito.mock(ValidatorUtilsDelegator.class);
        MifeValidator validator = Mockito.mock(MifeValidator.class);
        Mockito.when(mockValidatorUtilsDelegator.getValidatorForSubscriptionFromMessageContext(mockAxis2MessageContext)).thenReturn(validator);
        Mockito.when(validator.validate(mockAxis2MessageContext)).thenReturn(true);

        OperatorEndpoint mockEndpoint = Mockito.mock(OperatorEndpoint.class);
        EndpointReference mockEndpointReference = Mockito.mock(EndpointReference.class);
        Mockito.when(mockOcci.getOperatorEndpoint((OparatorEndPointSearchDTO) any())).thenReturn(mockEndpoint);

        Mockito.when(mockEndpoint.getEndpointref()).thenReturn(mockEndpointReference);
        Mockito.when(mockEndpointReference.getAddress()).thenReturn("http://10.7.0.103:8280/payment/SBcRDj%2F%2BM108gFCu1S56zw%3D%3D/transactions/amount");

        Mockito.when(mockPaymentExecutor.getSubResourcePath()).thenReturn("/SBcRDj%2F%2BM108gFCu1S56zw%3D%3D/transactions/amount");
        Mockito.when(mockPaymentExecutor.isUserAnonymization()).thenReturn(true);

        Mockito.when(mockPaymentUtil.isAggregator(mockAxis2MessageContext)).thenReturn(true);

        AmountChargeHandler amountChargeHandler = new AmountChargeHandler(mockOcci, mockPaymentService, mockPaymentExecutor,
                mockApiUtils, mockPaymentUtil, mockCarbonUtilsDelegator, mockFileReader, mockValidatorUtilsDelegator);
        boolean result = amountChargeHandler.handle(mockAxis2MessageContext);
        Assert.assertEquals(result, true);
    }


}
