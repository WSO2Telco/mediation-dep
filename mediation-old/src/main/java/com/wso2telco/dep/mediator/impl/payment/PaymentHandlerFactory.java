/*
 *
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package com.wso2telco.dep.mediator.impl.payment;

import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

public class PaymentHandlerFactory {

	private static Log log = LogFactory.getLog(PaymentHandlerFactory.class);

	public static PaymentHandler createPaymentHandler(String resourceURL, PaymentExecutor executor) {

		PaymentHandler paymentHandler = null;
		String transactionOperationStatus = null;
		if (log.isDebugEnabled())
			log.debug("createPaymentHandler -> Json string : " + executor.getJsonBody().toString());

		try {

			paymentHandler = process(resourceURL, executor);
		} catch (CustomException e) {

			log.error("createPaymentHandler -> Manipulating recived JSON Object: " + e);
			throw new CustomException(e.getErrcode(), "", new String[] { e.getErrvar()[0] });
		} catch (Exception e) {

			log.error("createPaymentHandler -> Manipulating recived JSON Object: " + e);
			throw new CustomException("SVC0002", "", new String[] { null });
		}

		return paymentHandler;
	}

	private static PaymentHandler process(String resourceURL, PaymentExecutor executor) throws Exception {

		PaymentHandler paymentHandler = null;
		String transactionOperationStatus = null;
		if (log.isDebugEnabled())
			log.debug("createPaymentHandler -> Json string : " + executor.getJsonBody().toString());

		String httpMethod = executor.getHttpMethod();

		if (httpMethod.equalsIgnoreCase("post")) {

			if (resourceURL.contains("amountReservation")) {

				if (log.isDebugEnabled())
					log.debug("Amount reservation transaction");
				String parts[] = resourceURL.split("/transactions/");
				String urlParts[] = parts[1].split("/");
				if (log.isDebugEnabled())
					log.debug("createPaymentHandler -> Payment url parts : " + urlParts.length);

				if (urlParts.length == 1) {

					if (log.isDebugEnabled())
						log.debug("createPaymentHandler -> Payment API type : Reserve an amount to charge");
					paymentHandler = new AmountReserveHandler(executor);
				} else if (urlParts.length == 2) {

					JSONObject objJSONObject = executor.getJsonBody();

					JSONObject objAmountReservationTransaction = (JSONObject) objJSONObject
							.get("amountReservationTransaction");

					if (objAmountReservationTransaction.get("transactionOperationStatus") != null) {

						transactionOperationStatus = nullOrTrimmed(
								objAmountReservationTransaction.get("transactionOperationStatus").toString());
						if (log.isDebugEnabled())
							log.debug("createPaymentHandler -> Transaction operation status" + transactionOperationStatus);

						if (transactionOperationStatus.equalsIgnoreCase("Reserved")) {

							if (log.isDebugEnabled())
								log.debug("createPaymentHandler -> Payment API type : Reserve an additional amount");
							paymentHandler = new AmountReserveAdditionalHandler(executor);

						} else if (transactionOperationStatus.equalsIgnoreCase("Charged")) {

							if (log.isDebugEnabled())
								log.debug("createPaymentHandler -> Payment API type : Charge against the reservation");
							paymentHandler = new AmountReserveChargeHandler(executor);

						} else if (transactionOperationStatus.equalsIgnoreCase("Released")) {

							if (log.isDebugEnabled())
								log.debug("createPaymentHandler -> Payment API type : Release the reservation");
							paymentHandler = new AmountReserveReleaseHandler(executor);

						} else {

							if (log.isDebugEnabled())
								log.debug("createPaymentHandler -> API Type Not found");
							throw new CustomException("SVC0002", "", new String[] { null });

						}
					} else {

						if (log.isDebugEnabled())
							log.debug("createPaymentHandler -> API Type Not found");
						throw new CustomException("SVC0002", "", new String[] { null });
					}
				} else {

					if (log.isDebugEnabled())
						log.debug("createPaymentHandler -> API Type Not found");
					throw new CustomException("SVC0002", "", new String[] { null });

				}
			} else if (resourceURL.contains("amount")) {

				JSONObject objJSONObject = executor.getJsonBody();

				if (!objJSONObject.has("amountTransaction")) {
                    log.error("createPaymentHandler -> API Type Not found");
                    throw new CustomException("SVC0002", "",
                            new String[] { "Missing mandatory parameter: amountTransaction" });
                }

				JSONObject objAmountTransaction = (JSONObject) objJSONObject.get("amountTransaction");

				if (!objAmountTransaction.has("transactionOperationStatus")) {

					if (log.isDebugEnabled())
						log.debug("createPaymentHandler -> API Type Not found");
					throw new CustomException("SVC0002", "",
							new String[] { "Missing mandatory parameter: transactionOperationStatus" });
				}

				if (!objAmountTransaction.get("transactionOperationStatus").equals("")) {

					transactionOperationStatus = nullOrTrimmed(
							objAmountTransaction.get("transactionOperationStatus").toString());
					if (log.isDebugEnabled())
						log.debug("createPaymentHandler -> Transaction operation status" + transactionOperationStatus);

					if (transactionOperationStatus.equalsIgnoreCase("Charged")) {

						if (log.isDebugEnabled())
							log.debug("createPaymentHandler -> Payment API type : Charge a user");
						paymentHandler = new AmountChargeHandler(executor);
					} else if (transactionOperationStatus.equalsIgnoreCase("Refunded")) {

						if (log.isDebugEnabled())
							log.debug("createPaymentHandler -> Payment API type : Refund a user");
						paymentHandler = new AmountRefundHandler(executor);
					} else {

						if (log.isDebugEnabled())
							log.debug("createPaymentHandler -> API Type Not found");
						throw new CustomException("SVC0002", "", new String[] { "Invalid transactionOperationStatus" });
					}
				} else {

					if (log.isDebugEnabled())
						log.debug("createPaymentHandler -> API Type Not found");
					throw new CustomException("SVC0002", "",
							new String[] { "Missing mandatory parameter: transactionOperationStatus" });
				}

				JSONObject objPaymentAmount = (JSONObject) objAmountTransaction.get("paymentAmount");
				JSONObject objChargingInformation = (JSONObject) objPaymentAmount.get("chargingInformation");

				if ((!objChargingInformation.has("currency") || objChargingInformation.get("currency").equals(""))&&
					(!objChargingInformation.isNull("amount") && objChargingInformation.has("amount") && !objChargingInformation.get("amount").equals(""))) {
					if (log.isDebugEnabled())
						log.debug("createPaymentHandler -> parameter not found.");
					throw new CustomException("SVC0002", "", new String[] { "Missing mandatory parameter: currency" });
				} else
					if ((objChargingInformation.isNull("amount") || !objChargingInformation.has("amount") || objChargingInformation.get("amount").equals(""))
						&& (objChargingInformation.has("currency")&& !objChargingInformation.get("currency").equals(""))) {
						if (log.isDebugEnabled())
							log.debug("createPaymentHandler -> parameter not found.");
					throw new CustomException("SVC0002", "", new String[] { "Missing mandatory parameter: amount" });
				} else if ((objChargingInformation.isNull("amount") || !objChargingInformation.has("amount") || objChargingInformation.get("amount").equals(""))
						&& (!objChargingInformation.has("currency") || objChargingInformation.get("currency").equals(""))) {
							if (!objChargingInformation.has("code") || objChargingInformation.get("code").equals("")) {
								if (log.isDebugEnabled())
									log.debug("createPaymentHandler -> parameter not found.");
								throw new CustomException("SVC0002", "",new String[] { "Missing mandatory parameter: amount or code" });
							}
				}

			} else {

				if (log.isDebugEnabled())
					log.debug("createPaymentHandler -> API Type Not found");
				throw new CustomException("SVC0002", "", new String[] { null });
			}
		} else if (httpMethod.equalsIgnoreCase("get")) {

			if (resourceURL.contains("/transactions/amount/")) {

				if (log.isDebugEnabled())
					log.debug("createPaymentHandler -> Payment API type : Query the status of a transaction");
				paymentHandler = new QueryPaymentStatusHandler(executor);
			} else if (resourceURL.contains("/transactions")) {

				if (log.isDebugEnabled())
					log.debug("createPaymentHandler -> Payment API type : List all transactions");
				paymentHandler = new ListTransactionsHandler(executor);
			} else {

				if (log.isDebugEnabled())
					log.debug("createPaymentHandler -> API Type Not found");
				throw new CustomException("SVC0002", "", new String[] { null });
			}
		} else {

			if (log.isDebugEnabled())
				log.debug("createPaymentHandler -> API Type Not found");
			throw new CustomException("SVC0002", "", new String[] { null });
		}

		return paymentHandler;
	}

	private static String nullOrTrimmed(String s) {

		String rv = null;
		if (s != null && s.trim().length() > 0) {

			rv = s.trim();
		}

		return rv;
	}
}