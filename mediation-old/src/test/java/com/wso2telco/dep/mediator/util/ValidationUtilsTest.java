package com.wso2telco.dep.mediator.util;

import com.google.common.base.Joiner;
import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.testng.annotations.Test;

/**
 * @author Salinda Karunarathna
 * @since 06/05/2019
 */
public class ValidationUtilsTest {

    private String msisdn = "94373524301";

    @Test
    public void testCompareMsisdn_PaymentEqualMsisdns_thenNoExceptions() {

        String jsonBody = "{\n" +
                "    \"amountTransaction\": {\n" +
                "        \"clientCorrelator\": \"123456:AIN123458\",\n" +
                "        \"endUserId\": \"tel:+94773524308\",\n" +
                "        \"paymentAmount\": {\n" +
                "            \"chargingInformation\": {\n" +
                "                \"amount\": \"20\",\n" +
                "                \"currency\": \"LKR\",\n" +
                "                \"description\": \"Alien Invaders Game\"\n" +
                "            },\n" +
                "            \"chargingMetaData\": null\n" +
                "        },\n" +
                "        \"referenceCode\": \"REF-123458\",\n" +
                "        \"transactionOperationStatus\": \"Charged\"\n" +
                "    }\n" +
                "}";

        ValidationUtils.compareMsisdn("/tel%3A%2B94773524308/transactions/amount", new JSONObject(jsonBody), APIType.PAYMENT);
    }

    @Test(expectedExceptions = CustomException.class)
    public void testCompareMsisdn_PaymentDifferentMsisdns() {

        String jsonBody = "{\n" +
                "    \"amountTransaction\": {\n" +
                "        \"clientCorrelator\": \"123456:AIN123458\",\n" +
                "        \"endUserId\": \"tel:+94773524309\",\n" +
                "        \"paymentAmount\": {\n" +
                "            \"chargingInformation\": {\n" +
                "                \"amount\": \"20\",\n" +
                "                \"currency\": \"LKR\",\n" +
                "                \"description\": \"Alien Invaders Game\"\n" +
                "            },\n" +
                "            \"chargingMetaData\": null\n" +
                "        },\n" +
                "        \"referenceCode\": \"REF-123458\",\n" +
                "        \"transactionOperationStatus\": \"Charged\"\n" +
                "    }\n" +
                "}";

        ValidationUtils.compareMsisdn("/tel%3A%2B94773524308/transactions/amount", new JSONObject(jsonBody), APIType.PAYMENT);
    }

    @Test(expectedExceptions = CustomException.class)
    public void testCompareMsisdn_PaymentURLMsisdnNull() {

        String jsonBody = "{\n" +
                "    \"amountTransaction\": {\n" +
                "        \"clientCorrelator\": \"123456:AIN123458\",\n" +
                "        \"endUserId\": \"tel:+94773524309\",\n" +
                "        \"paymentAmount\": {\n" +
                "            \"chargingInformation\": {\n" +
                "                \"amount\": \"20\",\n" +
                "                \"currency\": \"LKR\",\n" +
                "                \"description\": \"Alien Invaders Game\"\n" +
                "            },\n" +
                "            \"chargingMetaData\": null\n" +
                "        },\n" +
                "        \"referenceCode\": \"REF-123458\",\n" +
                "        \"transactionOperationStatus\": \"Charged\"\n" +
                "    }\n" +
                "}";

        ValidationUtils.compareMsisdn("/t/transactions/amount", new JSONObject(jsonBody), APIType.PAYMENT);
    }

    @Test
    public void testCompareMsisdn_UssdEqualMsisdns() {


        String jsonBody = "{  \n" +
                "\t   \"outboundUSSDMessageRequest\":{  \n" +
                "      \"address\":\"tel:+94373524300\",\n" +
                "      \"shortCode\":\"4343\",\n" +
                "      \"keyword\":\"3456\",\n" +
                "      \"outboundUSSDMessage\":\"Streaming video of the big fight\",\n" +
                "      \"clientCorrelator\":\"1236ssssa2a3\",\n" +
                "      \"responseRequest\":{  \n" +
                "         \"notifyURL\":\"http://52.70.50.209:8686/ussd/v1/inbound\",\n" +
                "         \"callbackData\":\"some-data-useful-to-the-requester\"\n" +
                "      },\n" +
                "      \"ussdAction\":\"mtinit\"\n" +
                "   }\n" +
                "}";

        ValidationUtils.compareMsisdn("/ussd/v1/outbound/tel%3A%2B94373524300", new JSONObject(jsonBody), APIType.USSD);
    }

    @Test(expectedExceptions = CustomException.class)
    public void testCompareMsisdn_UssdDifferentMsisdns() {


        String jsonBody = "{  \n" +
                "\t   \"outboundUSSDMessageRequest\":{  \n" +
                "      \"address\":\"tel:+94373524300\",\n" +
                "      \"shortCode\":\"4343\",\n" +
                "      \"keyword\":\"3456\",\n" +
                "      \"outboundUSSDMessage\":\"Streaming video of the big fight\",\n" +
                "      \"clientCorrelator\":\"1236ssssa2a3\",\n" +
                "      \"responseRequest\":{  \n" +
                "         \"notifyURL\":\"http://52.70.50.209:8686/ussd/v1/inbound\",\n" +
                "         \"callbackData\":\"some-data-useful-to-the-requester\"\n" +
                "      },\n" +
                "      \"ussdAction\":\"mtinit\"\n" +
                "   }\n" +
                "}";

        ValidationUtils.compareMsisdn("/ussd/v1/outbound/tel%3A%2B94373524301", new JSONObject(jsonBody), APIType.USSD);
    }

    @Test
    public void testGetMsisdnNumber_TEL_1() {

        String msisdnWithPrefix = getMsisdnWithPrefix(MSISDNConstants.TEL_1);
        Assertions.assertThat(ValidationUtils.getMsisdnNumber(msisdnWithPrefix)).isEqualTo(msisdn);
    }

    @Test
    public void testGetMsisdnNumber_TEL_2() {

        String msisdnWithPrefix = getMsisdnWithPrefix(MSISDNConstants.TEL_2);
        Assertions.assertThat(ValidationUtils.getMsisdnNumber(msisdnWithPrefix)).isEqualTo(msisdn);
    }

    @Test
    public void testGetMsisdnNumber_TEL_3() {

        String msisdnWithPrefix = getMsisdnWithPrefix(MSISDNConstants.TEL_3);
        Assertions.assertThat(ValidationUtils.getMsisdnNumber(msisdnWithPrefix)).isEqualTo(msisdn);
    }

    @Test
    public void testGetMsisdnNumber_PLUS() {

        String msisdnWithPrefix = getMsisdnWithPrefix(MSISDNConstants.PLUS);
        Assertions.assertThat(ValidationUtils.getMsisdnNumber(msisdnWithPrefix)).isEqualTo(msisdn);
    }

    @Test
    public void testGetMsisdnNumber_ETEL_1() {

        String msisdnWithPrefix = getMsisdnWithPrefix(MSISDNConstants.ETEL_1);
        Assertions.assertThat(ValidationUtils.getMsisdnNumber(msisdnWithPrefix)).isEqualTo(msisdn);
    }

    @Test
    public void testGetMsisdnNumber_ETEL_2() {

        String msisdnWithPrefix = getMsisdnWithPrefix(MSISDNConstants.ETEL_2);
        Assertions.assertThat(ValidationUtils.getMsisdnNumber(msisdnWithPrefix)).isEqualTo(msisdn);
    }

    @Test
    public void testGetMsisdnNumber_ETEL_3() {

        String msisdnWithPrefix = getMsisdnWithPrefix(MSISDNConstants.ETEL_3);
        Assertions.assertThat(ValidationUtils.getMsisdnNumber(msisdnWithPrefix)).isEqualTo(msisdn);
    }

    private String getMsisdnWithPrefix(String prefix) {
        return Joiner.on("").join(prefix, msisdn);
    }
}
