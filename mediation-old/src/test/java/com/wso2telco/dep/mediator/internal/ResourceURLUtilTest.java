package com.wso2telco.dep.mediator.internal;


import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;


public class ResourceURLUtilTest {

    /**
     * The delimiter for query params is a question mark (?). If there is more than one question mark it should be
     * treated
     * as a value and not a delimiter
     *
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testGetParamPairs_twoQuestionMarks_returnsTwoParams() throws UnsupportedEncodingException {
        ResourceURLUtil resourceURLUtil = new ResourceURLUtil();
        String url = "http://localhost:8280/location/v1/queries/location?address=tel:+94373524300?&requestedAccuracy" +
                "=10";

        String[] params = resourceURLUtil.getParamPairs(url);
        Assertions.assertThat(params.length).isEqualTo(2);

    }

    //Beginning of prefix testing

    @Test
    public void testGetParamPairs_rawPrefix1_returnsUnchangedMsisdn() throws UnsupportedEncodingException {
        ResourceURLUtil resourceURLUtil = new ResourceURLUtil();
        String msisdn = "tel:+94373524300";
        String url = "http://localhost:8280/location/v1/queries/location?address=" + msisdn + "&requestedAccuracy=10";

        String[] params = resourceURLUtil.getParamPairs(url);
        Assertions.assertThat(params[0].split("=")[1]).isEqualTo(msisdn);

    }

    @Test
    public void testGetParamPairs_rawPrefix2_returnsUnchangedMsisdn() throws UnsupportedEncodingException {
        ResourceURLUtil resourceURLUtil = new ResourceURLUtil();
        String msisdn = "tel:94373524300";
        String url = "http://localhost:8280/location/v1/queries/location?address=" + msisdn + "&requestedAccuracy=10";

        String[] params = resourceURLUtil.getParamPairs(url);
        Assertions.assertThat(params[0].split("=")[1]).isEqualTo(msisdn);

    }

    @Test
    public void testGetParamPairs_rawPrefix3_returnsUnchangedMsisdn() throws UnsupportedEncodingException {
        ResourceURLUtil resourceURLUtil = new ResourceURLUtil();
        String msisdn = "+94373524300";
        String url = "http://localhost:8280/location/v1/queries/location?address=" + msisdn + "&requestedAccuracy=10";

        String[] params = resourceURLUtil.getParamPairs(url);
        Assertions.assertThat(params[0].split("=")[1]).isEqualTo(msisdn);

    }

    @Test
    public void testGetParamPairs_encodedPrefix1_returnsDecodedMsisdn() throws UnsupportedEncodingException {
        ResourceURLUtil resourceURLUtil = new ResourceURLUtil();
        String encodedMsisdn = "tel%3A%2B94373524300";
        String msisdn = "tel:+94373524300";
        String url = "http://localhost:8280/location/v1/queries/location?address=" + encodedMsisdn + "&requestedAccuracy=10";

        String[] params = resourceURLUtil.getParamPairs(url);
        Assertions.assertThat(params[0].split("=")[1]).isEqualTo(msisdn);

    }


    @Test
    public void testGetParamPairs_encodedPrefix2_returnsDecodedMsisdn() throws UnsupportedEncodingException {
        ResourceURLUtil resourceURLUtil = new ResourceURLUtil();
        String encodedMsisdn = "tel%3A94373524300";
        String msisdn = "tel:94373524300";
        String url = "http://localhost:8280/location/v1/queries/location?address=" + encodedMsisdn + "&requestedAccuracy=10";

        String[] params = resourceURLUtil.getParamPairs(url);
        Assertions.assertThat(params[0].split("=")[1]).isEqualTo(msisdn);

    }

    @Test
    public void testGetParamPairs_encodedPrefix3_returnsDecodedMsisdn() throws UnsupportedEncodingException {
        ResourceURLUtil resourceURLUtil = new ResourceURLUtil();
        String encodedMsisdn = "%2B94373524300";
        String msisdn = "+94373524300";
        String url = "http://localhost:8280/location/v1/queries/location?address=" + encodedMsisdn + "&requestedAccuracy=10";

        String[] params = resourceURLUtil.getParamPairs(url);
        Assertions.assertThat(params[0].split("=")[1]).isEqualTo(msisdn);

    }

    //End of prefix testing


}
