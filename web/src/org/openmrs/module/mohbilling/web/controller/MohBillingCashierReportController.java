package org.openmrs.module.mohbilling.web.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.mohbilling.GlobalPropertyConfig;
import org.openmrs.module.mohbilling.ParametersConversion;
import org.openmrs.module.mohbilling.businesslogic.BillPaymentUtil;
import org.openmrs.module.mohbilling.businesslogic.InsuranceUtil;
import org.openmrs.module.mohbilling.businesslogic.ReportsUtil;
import org.openmrs.module.mohbilling.model.AllServicesRevenue;
import org.openmrs.module.mohbilling.model.BillPayment;
import org.openmrs.module.mohbilling.model.Consommation;
import org.openmrs.module.mohbilling.model.GlobalBill;
import org.openmrs.module.mohbilling.model.HopService;
import org.openmrs.module.mohbilling.model.PaidServiceBill;
import org.openmrs.module.mohbilling.model.PaidServiceRevenue;
import org.openmrs.module.mohbilling.model.ServiceRevenue;
import org.openmrs.module.mohbilling.service.BillingService;
import org.openmrs.web.WebConstants;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

public class MohBillingCashierReportController extends
		ParameterizableViewController {
	
	protected final Log log = LogFactory.getLog(getClass());

	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.ParameterizableViewController#handleRequestInternal(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		
		ModelAndView mav = new ModelAndView();
		if (request.getParameter("formStatus") != null
				&& !request.getParameter("formStatus").equals("")) {
			String startDateStr = request.getParameter("startDate");
			String startHourStr = request.getParameter("startHour");
			String startMinStr = request.getParameter("startMinute");
			
			String endDateStr = request.getParameter("endDate");
			String endHourStr = request.getParameter("endHour");
			String endMinuteStr = request.getParameter("endMinute");
			
			String collectorStr = null;
			String insuranceStr = null;
			String thirdPartyStr = null;
			
			if(request.getParameter("cashCollector")!=null && !request.getParameter("cashCollector").equals(""))
				collectorStr= request.getParameter("cashCollector");

			if(request.getParameter("insuranceId")!=null && request.getParameter("insuranceId").equals(""))
				insuranceStr = request.getParameter("insuranceId");

			if(request.getParameter("thirdPartyId")!=null && !request.getParameter("thirdPartyId").equals(""))
			 thirdPartyStr = request.getParameter("thirdPartyId");
			
			 Object[] params = ReportsUtil.getReportParameters(request, startDateStr, startHourStr, startMinStr, endDateStr, endHourStr, endMinuteStr, collectorStr, insuranceStr, thirdPartyStr);
			
			 List<PaidServiceRevenue> paidServiceRevenues = new ArrayList<PaidServiceRevenue>();

			 Date startDate = (Date) params[0];
			 Date endDate = (Date) params[1];
			 User collector =  (User) params[2];

			
			 List<BillPayment> payments = BillPaymentUtil.getAllPaymentByDatesAndCollector(startDate, endDate,collector);
			 BigDecimal totalReceivedAmount = new BigDecimal(0);
			 for (BillPayment bp : payments) {
				totalReceivedAmount=totalReceivedAmount.add(bp.getAmountPaid());
			}

			 try {
				 List<PaidServiceBill> paidItems = BillPaymentUtil.getPaidItemsByBillPayments(payments); 
				 List<HopService> reportColumns = GlobalPropertyConfig.getHospitalServiceByCategory("mohbilling.cashierReportColumns");
	     		 for (HopService hs : reportColumns) {
					if(ReportsUtil.getPaidServiceRevenue(paidItems,hs.getName())!=null)
						paidServiceRevenues.add(ReportsUtil.getPaidServiceRevenue(paidItems,hs.getName()));
				}
	 			mav.addObject("totalReceivedAmount", totalReceivedAmount);
				mav.addObject("paidServiceRevenues", paidServiceRevenues);
				mav.addObject("reportMsg", collector.getPersonName()+" From "+startDateStr+" To "+endDateStr);
				
			} catch (Exception e) {
				request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
						"No payment found !");
				log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> "+e.getMessage());
			}

	}	
		mav.addObject("insurances",InsuranceUtil.getAllInsurances());
		mav.addObject("thirdParties",Context.getService(BillingService.class).getAllThirdParties());
		mav.setViewName(getViewName());
		return mav;
}
}
