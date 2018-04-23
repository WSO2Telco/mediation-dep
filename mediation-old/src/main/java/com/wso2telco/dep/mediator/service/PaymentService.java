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
package com.wso2telco.dep.mediator.service;

import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.core.dbutils.exception.GenaralError;
import com.wso2telco.dep.mediator.dao.PaymentDAO;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class PaymentService {

	/** The Constant log. */
	private final Log log = LogFactory.getLog(PaymentService.class);

	PaymentDAO paymentDAO;
	private static List<String> categories = null;

	{
		paymentDAO = new PaymentDAO();
	}

	@SuppressWarnings("unchecked")
	public List<String> getValidPayCategories() throws BusinessException {

		try {

			if(categories==null || categories.isEmpty()){
				categories = paymentDAO.getValidPayCategories();
			}

		} catch (Exception e) {

			throw new BusinessException(GenaralError.INTERNAL_SERVER_ERROR);
		}

		if (categories != null) {

			return categories;
		} else {

			return Collections.emptyList();
		}
	}
}
