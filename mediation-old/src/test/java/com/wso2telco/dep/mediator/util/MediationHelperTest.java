package com.wso2telco.dep.mediator.util;

import com.wso2telco.core.dbutils.exception.BusinessException;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.assertj.core.api.Assertions;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

/**
 * @author Salinda Karunarathna
 * @since 5/14/19
 */
public class MediationHelperTest {

    @Test
    public void testGetApplicationId_whenMessageContextHasAppIdProperty() {
        try {
            MessageContext mockMessageContext = Mockito.mock(MessageContext.class);
            Axis2MessageContext mockAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            when(mockAxis2MessageContext.getAxis2MessageContext()).thenReturn(mockMessageContext);
            when(mockAxis2MessageContext.getProperty(MediationHelper.APP_ID)).thenReturn("App_Id");

            String applicationId = MediationHelper.getInstance().getApplicationId(mockAxis2MessageContext);
            Assertions.assertThat(applicationId).isEqualTo("App_Id");
        } catch (BusinessException e) {
            fail();
        }
    }

    @Test
    public void testGetApplicationId_whenMessageContextHasNoAppIdPropertyAndContainsInJWT() {
        try {
            MessageContext mockMessageContext = Mockito.mock(MessageContext.class);
            Axis2MessageContext mockAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);

            when(mockAxis2MessageContext.getAxis2MessageContext()).thenReturn(mockMessageContext);
            when(mockAxis2MessageContext.getProperty(MediationHelper.APP_ID)).thenReturn(null);
            Map<String, String> headersMap = new HashMap<>();
            headersMap.put("x-jwt-assertion", "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IlVCX0JReTJIRlYzRU1UZ3E2NFEtMVZpdFliRSJ9.eyJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9hcHBsaWNhdGlvbnRpZXIiOiJVbmxpbWl0ZWQiLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9rZXl0eXBlIjoiUFJPRFVDVElPTiIsImh0dHA6XC9cL3dzbzIub3JnXC9jbGFpbXNcL3ZlcnNpb24iOiJ2MSIsImlzcyI6IndzbzIub3JnXC9wcm9kdWN0c1wvYW0iLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9hcHBsaWNhdGlvbm5hbWUiOiJEZWZhdWx0QXBwbGljYXRpb24iLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9lbmR1c2VyIjoiYWRtaW5AY2FyYm9uLnN1cGVyIiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvZW5kdXNlclRlbmFudElkIjoiLTEyMzQiLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC9zdWJzY3JpYmVyIjoiYWRtaW4iLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC90aWVyIjoiRGVmYXVsdCIsImV4cCI6MTU1NjUzMjQxNCwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBwbGljYXRpb25pZCI6IjEiLCJodHRwOlwvXC93c28yLm9yZ1wvY2xhaW1zXC91c2VydHlwZSI6IkFQUExJQ0FUSU9OIiwiaHR0cDpcL1wvd3NvMi5vcmdcL2NsYWltc1wvYXBpY29udGV4dCI6IlwvcGF5bWVudFwvdjEifQ==.F/5iCja6NTzr/jVYQEOrDewi5cDjBo6bB6c/pWzVEku636PMB1u6qdu+mQovrQnZWcl1jgoHxTOoIWDE4uRXOPNrLHMd9yxmquAtyGa9hJ8cu8SLw2GcgCpdAaAQGB57Pxc0HV0oUR19UZTT/IuokblOqTa+uPuTQ1USfpx4gDMZDgZEw7VPPA0CZ6SFut6pp8oIJZctYOHOmVx3X9LF/jV+fBODaxVXzxc6hIi3v7PlEk8egJNjfLgDpSQcpdJYk9SzxP/RRFB3CFGCmZlqCJkg8DdyLRYM1ZuASke72K9ytRyzw5WfVOZntmzrh8CoAjT/9SElGUwrCgdqhtWN1Q==");

            when(mockMessageContext.getProperty(MessageContext.TRANSPORT_HEADERS)).thenReturn(headersMap);

            String applicationId = MediationHelper.getInstance().getApplicationId(mockAxis2MessageContext);

            //Need this verification due to bad design of parameter modification is also doing inside the method.
            //The original method need a refactoring since it is violating single responsibility principle.
            verify(mockAxis2MessageContext, times(1)).setProperty(MediationHelper.APP_ID, "1");
            Assertions.assertThat(applicationId).isEqualTo("1");

        } catch (BusinessException e) {
            fail();
        }
    }

    @Test
    public void testGetApplicationId_whenMessageContextHasNoAppIdPropertyAndHeadersMapIsNull_thenApplicationIdNull() {
        try {
            MessageContext mockMessageContext = Mockito.mock(MessageContext.class);
            Axis2MessageContext mockAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);

            when(mockAxis2MessageContext.getAxis2MessageContext()).thenReturn(mockMessageContext);
            when(mockAxis2MessageContext.getProperty(MediationHelper.APP_ID)).thenReturn(null);

            when(mockMessageContext.getProperty(MessageContext.TRANSPORT_HEADERS)).thenReturn(null);

            assertNull(MediationHelper.getInstance().getApplicationId(mockAxis2MessageContext));

        } catch (BusinessException e) {
            fail();
        }
    }

    @Test(expectedExceptions = BusinessException.class, expectedExceptionsMessageRegExp = "Can't find 'x-jwt-assertion' header in request")
    public void testGetApplicationId_whenMessageContextHasNoAppIdPropertyAndMapHasNoValueForJWT()
            throws BusinessException {
        MessageContext mockMessageContext = Mockito.mock(MessageContext.class);
        Axis2MessageContext mockAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);

        when(mockAxis2MessageContext.getAxis2MessageContext()).thenReturn(mockMessageContext);
        when(mockAxis2MessageContext.getProperty(MediationHelper.APP_ID)).thenReturn(null);
        Map<String, String> headersMap = new HashMap<>();

        when(mockMessageContext.getProperty(MessageContext.TRANSPORT_HEADERS)).thenReturn(headersMap);

        //This will throw a BusinessException
        String applicationId = MediationHelper.getInstance().getApplicationId(mockAxis2MessageContext);
    }

    @Test(expectedExceptions = BusinessException.class, expectedExceptionsMessageRegExp = "Error retriving application id")
    public void testGetApplicationId_whenOccurringJsonException() throws BusinessException {
        MessageContext mockMessageContext = Mockito.mock(MessageContext.class);
        Axis2MessageContext mockAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);

        when(mockAxis2MessageContext.getAxis2MessageContext()).thenReturn(mockMessageContext);
        when(mockAxis2MessageContext.getProperty(MediationHelper.APP_ID)).thenReturn(null);
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("x-jwt-assertion", "dfaf.eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImp0aSI6IjEyNzk0MmJjLWYyNmMtNGMzZS04YWUwLTFmMWQxN2QwYzZhOCIsImlhdCI6MTU1NzgyODMxMiwiZXhwIjoxNTU3ODMxOTEyfQ.yQS-ccghtQMeG5oDOmwdieOzbaFj6c1C3GRo73YymVc");

        when(mockMessageContext.getProperty(MessageContext.TRANSPORT_HEADERS)).thenReturn(headersMap);

        //This will throw a BusinessException
        String applicationId = MediationHelper.getInstance().getApplicationId(mockAxis2MessageContext);
    }
}
