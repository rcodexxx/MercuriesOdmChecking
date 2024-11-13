package com.firsttech.insurance.odmchecking.service.cronJob;

import com.firsttech.insurance.odmchecking.service.EmailService;
import com.firsttech.insurance.odmchecking.service.SmsService;
import com.firsttech.insurance.odmchecking.service.utils.FileUtil;
import com.firsttech.insurance.odmchecking.service.utils.HttpUtil;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;


@Service
public class OdmHealthCheckJob {
	private final static Logger logger = LoggerFactory.getLogger(OdmHealthCheckJob.class);
	
	@Autowired
	private Environment environment;
	
	@Autowired
	private SmsService smsService;

	@Autowired
	private EmailService emailService;
	
	@Scheduled(cron = "0 0/5 * * * ?")
	public void odmHealthChecking() {
		boolean isAlive = false;
		System.out.println("[CRON JOB] odmHealthChecking: start to do health checking for ODM");
		
		// 取得 IP 資訊檔案位置
		String infoFilePath = environment.getProperty("current.ip.info");
		Map<String, String> infoMap = FileUtil.getLocalIpInfo(infoFilePath);
		String currentIP = infoMap.get("local.ip");
		String odmCheckUrl = infoMap.get("target.odm.url"); // 取得要驗證 ODM 的 URL
		System.out.println("[CRON JOB] currentIP: " + currentIP);
		System.out.println("[CRON JOB] odmCheckUrl: " + odmCheckUrl);
		
		if (currentIP == null || odmCheckUrl == null) {
			System.out.println("無法在設定檔中找到正確的 ODM check 連結");
			return;
		}
		
		// 對 ODM 做 health check
        try {
        	String nbTestJson = "{\"inParam\":{\"workday\":{},\"policy\":{\"agent\":[],\"insured\":[],\"beneficiary\":[],\"fund\":[],\"payment\":[],\"bankData\":{\"mcPoSeq\":\"null\",\"policyNo\":\"null\",\"coverageNo\":\"null\",\"mcnbiNoticeType\":\"null\",\"planCode\":\"null\",\"rateScale\":\"null\",\"appApplyDate\":\"1900-01-01\",\"mcReceiveNo\":\"null\",\"mcActionType\":\"null\",\"popjExistInd\":\"null\",\"mpCode\":\"null\",\"mcCode\":\"null\",\"branchCode\":\"null\",\"mcMailType\":\"null\",\"popjZipCode\":\"null\",\"popjAddress\":\"null\",\"mcIdentityInd\":\"null\",\"mcMailInd\":\"null\",\"mcpjExistInd\":\"null\",\"mpObjInd\":\"null\",\"mpBgnDate\":\"1900-01-01\",\"mpEndDate\":\"1900-01-01\",\"mcReceNo\":\"null\",\"planAbbrCode\":\"null\",\"firstCreditInd\":\"null\",\"firstCreditNoteInd\":\"null\",\"mcctCallOut\":\"null\",\"mcagLogInInd\":\"null\",\"mcOtherNo\":\"null\",\"mcOtherNoInd\":\"null\",\"mculStsCode\":\"null\",\"mcctsalesind\":\"null\",\"mcag1Cnt\":0,\"mcag2Cnt\":0,\"mc21PoisInd\":\"null\"},\"legalGuardian\":{\"clientId\":\"null\",\"names\":\"null\",\"idInd\":\"null\",\"birthDate\":\"1900-01-01\"},\"applicant\":{\"bankAccount\":[],\"fatca\":[],\"o1OldInFo\":[],\"o1OldPlanInFo\":[],\"h9000Names\":\"null\",\"h9000Sex\":\"null\",\"h9000BirthDate\":\"1900-01-01\",\"h9000ClientId\":\"null\",\"mailAddress\":\"null\",\"mailZipCod\":\"null\",\"mailAddressIaerA\":\"null\",\"mailAddressIaerB\":\"null\",\"mailAddressIaerC\":\"null\",\"mailAddressMc\":\"null\",\"address\":\"null\",\"zipCode\":\"null\",\"homeAddressIaerA\":\"null\",\"homeAddressIaerB\":\"null\",\"homeAddressIaerC\":\"null\",\"homeAddressMc\":\"null\",\"address1\":\"null\",\"birthDate\":\"1900-01-01\",\"oldBirthDate\":\"1900-01-01\",\"blackstsdate\":\"1900-01-01\",\"sex\":\"null\",\"oldSex\":\"null\",\"clientId\":\"null\",\"pomrClientId\":\"null\",\"names\":\"null\",\"oldNames\":\"null\",\"occupationCode\":\"null\",\"tel1\":\"null\",\"tel2\":\"null\",\"mobilePhone\":\"null\",\"mliAgntInd\":\"null\",\"mliHrcmInd\":\"null\",\"mliPccmInd\":\"null\",\"terrInd\":\"null\",\"riskLevel\":\"null\",\"riskScore\":-8.88888888E8,\"itemValueNew\":\"null\",\"riskSuitSeq\":\"null\",\"clientIdent\":\"null\",\"idInd\":\"null\",\"signPtnCard\":\"null\",\"rfmScore\":-888888888,\"o1Id\":\"null\",\"nbktReason\":\"null\",\"applAge\":-888888888,\"realAge\":-888888888,\"invsRiskDegree\":\"null\",\"mbdaRiskScore\":-8.88888888E8,\"validAgtSts\":\"null\",\"pczaExistInd\":\"null\",\"pcczZipInd\":\"null\",\"payableNotReceiveInd\":\"null\",\"hometown\":\"null\",\"blacklisttype\":\"null\",\"reducepremIn6mInd\":\"null\",\"occupationLevel\":\"null\",\"blackststime\":\"null\",\"homeAddressMc01\":0,\"homeAddressMc02\":0,\"homeAddressMc03\":0,\"mailAddressMc01\":0,\"mailAddressMc02\":0,\"mailAddressMc03\":0,\"eaddressIaerE\":\"null\",\"etelIaerH01\":0,\"etelIaerH02\":0,\"eaddressIaerG\":\"null\",\"etelIaerH\":\"null\",\"etelIaerF\":\"null\"},\"modx\":\"null\",\"appApplyDate\":\"1900-01-01\",\"appReceiveDate\":\"1900-01-01\",\"mailAddrInd\":\"null\",\"addrInd\":\"null\",\"miscSusp\":-8.88888888E8,\"chkMiseSusp\":-8.88888888E8,\"firstAutoInd\":\"null\",\"currency\":\"null\",\"insuranceType\":\"null\",\"relaWithOwner\":\"null\",\"relaWithClient\":\"null\",\"height\":-8.88888888E8,\"weight\":-8.88888888E8,\"incomeF\":-8.88888888E8,\"medicalFlag\":\"null\",\"divOption\":\"null\",\"nfOption\":\"null\",\"polfC\":\"null\",\"batchPoIssueDate\":\"1900-01-01\",\"poIssueDate\":\"1900-01-01\",\"freeLookOpt\":\"null\",\"groupCode\":\"null\",\"annuityType\":\"null\",\"freeExamInd\":\"null\",\"advReceInd\":\"null\",\"mculInd\":\"null\",\"nbflInd\":\"null\",\"method\":\"null\",\"deductUnit\":\"null\",\"employeeId\":\"null\",\"employeeName\":\"null\",\"relaWithEmp\":\"null\",\"empCollectInd\":\"null\",\"coverageCnt\":-8.88888888E8,\"mailAddrExistInd\":\"null\",\"homeAddrExistInd\":\"null\",\"bpsqBAInd\":\"null\",\"ac20xInputDate\":\"1900-01-01\",\"nbxhExistInd\":\"null\",\"relationO1I1\":\"null\",\"policyNo\":\"null\",\"productInd\":\"null\",\"bmiClass\":\"null\",\"nbhwClass\":\"null\",\"incomeI\":-8.88888888E8,\"incomeIWithinSixMonths\":-8.88888888E8,\"incomeFWithinSixMonths\":-8.88888888E8,\"divOption2\":\"null\",\"appInputDate\":\"1900-01-01\",\"premDiscPerc\":-8.88888888E8,\"chkMliCrCardInd\":\"null\",\"chkNbrnDupInd\":\"null\",\"payDateStart\":\"1900-01-01\",\"periodCertain\":-888888888,\"periodCertainAnkd\":\"null\",\"payMethod\":\"null\",\"divbackSts\":\"null\",\"invsAvailCnt\":-888888888,\"planPrem\":-8.88888888E8,\"fyBillingInd\":\"null\",\"payModx\":-888888888,\"payModxAnkd\":\"null\",\"coiSeqInd\":\"null\",\"saMultMin\":-8.88888888E8,\"saMultMax\":-8.88888888E8,\"poStsCode\":\"null\",\"anpayInd\":-888888888,\"divPayFreq\":\"null\",\"cpiOptionInd\":\"null\",\"anpayType\":\"null\",\"caDate\":\"1900-01-01\",\"divbackType\":\"null\",\"modePrem\":-8.88888888E8,\"poprFirstAutoInd\":\"null\",\"paidDate\":\"1900-01-01\",\"poRenewCnt\":-888888888,\"achExistInd\":\"null\",\"nbni2yAddTotalAmt\":0.0,\"nbni2yLifeTotalAmt\":0.0,\"nbniMedTotalAmt\":0.0,\"notSuitForAwl1\":\"null\",\"calcAnnuMppoiModePrem\":-8.88888888E8,\"calcAnnuBprem\":-8.88888888E8,\"calcMppoiModePrem\":-8.88888888E8,\"benfTotalRatio\":0.0,\"calcTargetPrem\":-8.88888888E8,\"wBackPoIssueDateInd\":\"null\",\"freeLookPlanCodeInd\":\"null\",\"dateC\":\"1900-01-01\",\"ivInvestDate\":\"1900-01-01\",\"invsCodeTotalRatio\":0.0,\"benfForDTotalRatio\":0.0,\"benfForMTotalRatio\":0.0,\"vfldfFeldCalcRef\":\"null\",\"calcMppoiRiderPrem\":-8.88888888E8,\"calcMppoiSpecialModePrem\":-8.88888888E8,\"miscSuspGreaterThan0\":\"null\",\"poModePrem\":-8.88888888E8,\"nbbkExistInd\":\"null\",\"nbstStnapendDeadLine\":\"1900-01-01\",\"mcetStnapendDeadLine\":\"1900-01-01\",\"nbctPremS\":-8.88888888E8,\"nbabRemitDiscSw\":\"null\",\"agntTotalRatio\":0.0,\"nbusCallOut\":\"null\",\"calcAnnuMpcoiCoModePrem\":-8.88888888E8,\"calcMppoiTotalAnnuPrem\":-8.88888888E8,\"nbni2yMedTotalAmt\":0.0,\"nbehCodeList\":[],\"indeductUnit\":\"null\",\"exchangeRate\":-8.88888888E8,\"basicFreeExam\":\"null\",\"targetPrem\":-8.88888888E8,\"poPremSusp\":-8.88888888E8,\"ipaIssueDate\":\"1900-01-01\",\"ipaAutoInd\":\"null\",\"bestAgntQualification\":\"null\",\"nbulProcessDate\":\"1900-01-01\",\"nbulProcessTime\":\"null\",\"nbflProcessDate\":\"1900-01-01\",\"nbflProcessTime\":\"null\",\"poprReceiveDate\":\"1900-01-01\",\"notExpiredTotalPrem\":-8.88888888E8,\"chkPocc3mInd\":\"null\",\"anpayStrDate\":\"1900-01-01\",\"relatedKey\":\"null\",\"nbflbatchtime\":\"null\",\"nbflbatchdate\":\"1900-01-01\",\"basicPlanCode\":\"null\",\"basicRateScale\":\"null\",\"rateAge\":0,\"rateSex\":\"null\",\"rateMedi\":\"null\",\"ratePlanCode\":\"null\",\"rateRateScale\":\"null\",\"blankind\":\"null\",\"blankdate\":\"1900-01-01\",\"blankfairind\":\"null\",\"autoPayCancelDiscountD\":\"1900-01-01\",\"u105ReceiveBeginD\":\"1900-01-01\",\"i1BMI\":0.0,\"rmind\":\"null\",\"awl1Ind\":\"null\",\"jcaddTotalAmt\":-8.88888888E8,\"lvlamt\":-8.88888888E8,\"jcsnTotalAmt\":-8.88888888E8,\"bf33Ind\":\"null\",\"jclifeTotalAmt\":-8.88888888E8,\"jcmedTotalAmt\":-8.88888888E8,\"targetPremMin\":-8.88888888E8,\"u105ApplyBeginD\":\"1900-01-01\",\"ipadinsuredInd\":\"null\",\"i1AddrInd\":\"null\",\"i1addrExistInd\":\"null\",\"lvlind\":\"null\",\"u105ReceiveEndD\":\"1900-01-01\",\"targetPremMax\":-8.88888888E8,\"u105ApplyEndD\":\"1900-01-01\"},\"information\":[],\"bornWeight\":-8.88888888E8,\"bornWeeks\":-8.88888888E8,\"preemieInd\":\"null\",\"incubatorInd\":\"null\",\"projectCode\":\"null\",\"nbpjExistInd\":\"null\",\"nbpjBeginDate\":\"1900-01-01\",\"nbpjEndDate\":\"1900-01-01\",\"nbErrCode\":\"null\",\"modx\":\"null\",\"method\":\"null\",\"medicalFlag\":\"null\",\"premWhenIssue\":-8.88888888E8,\"planPrem\":-8.88888888E8,\"existInd\":\"null\",\"proposalPremWhenIssue\":-8.88888888E8,\"rateSex\":\"null\",\"rateAge\":-888888888,\"planCode\":\"null\",\"faceAmt\":-8.88888888E8,\"targetPrem\":-8.88888888E8,\"proposalPlanPrem\":-8.88888888E8,\"uidaClientId\":\"null\",\"uidaBirthDate\":\"1900-01-01\",\"uidaInputDate\":\"1900-01-01\",\"formId\":\"null\",\"receiveNo\":\"null\",\"chkNB50Nbjc01\":\"null\",\"currency\":\"null\",\"deductUnit\":\"null\",\"pcduExistInd\":\"null\",\"names\":\"null\",\"clientId\":\"null\",\"ac201ExistInd\":\"null\",\"oldName\":\"null\",\"applyBegDate\":\"1900-01-01\",\"applyEndDate\":\"1900-01-01\",\"appVers\":\"null\",\"begDate\":\"1900-01-01\",\"endDate\":\"1900-01-01\",\"multiIdNo\":\"null\",\"pregnancyWeeks\":-888888888,\"receiveDate\":\"1900-01-01\",\"stepDate\":\"1900-01-01\",\"bpcmDocLackInd\":\"null\",\"freeLookFormId\":\"null\",\"freeLookNotifyInd1\":\"null\",\"freeLookClientId\":\"null\",\"freeLookInputDate\":\"1900-01-01\",\"freeLookExistInd\":\"null\",\"freeLookSignDate\":\"1900-01-01\",\"bpqiServiceBusiness\":\"null\",\"uipcFCInd\":\"null\",\"transNo\":\"null\",\"policyNo\":\"null\",\"serviceNo\":\"null\",\"txnSeq\":\"null\",\"sequence\":\"null\",\"business\":\"null\",\"channel\":\"null\",\"station\":\"null\",\"swtOpt\":\"null\",\"progType\":\"null\",\"batchDate\":\"1900-01-01\",\"processUser\":\"null\",\"argTxnMb\":\"null\",\"nj81_date\":\"1900-01-01\",\"nj88_ind\":\"null\",\"nj81_ind\":\"null\",\"nj22Ind\":\"null\",\"qa80Ind\":\"null\",\"qa82Ind\":\"null\"}}";
        	Map<String, String> headerMap = new HashMap<>();
        	headerMap.put("Accept", "application/json");
        	headerMap.put("Content-type", "application/json");
        	
            HttpResponse response = HttpUtil.httpRequestPost(odmCheckUrl, nbTestJson, headerMap);
//			String returnBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			int statusCode = response.getStatusLine().getStatusCode();
			
			if (statusCode == HttpStatus.OK.value()) {
				isAlive = true;
			} 
			System.out.println("[CRON JOB] odmHealthChecking: ODM checking result => statusCode: " + statusCode);

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException e) {
            System.out.println("[CRON JOB] odmHealthChecking: ODM (" + odmCheckUrl + ") Health Checking 發生錯誤, 錯誤訊息: " + e.getMessage());
		}

        // 如果確認失敗計送通知
		if (!isAlive) {
			// email 通知
			boolean isEmailSuccess = emailService.sendMail();
			System.out.println("[CRON JOB] 提醒EMAIL發送結果: " + (isEmailSuccess ? "成功" : "失敗"));
			
			// 簡訊 通知
			smsService.sendSMS();
		} else {
			System.out.println("[CRON JOB] 於 " + new Date() + "確認 ODM 運作正常");
		}

		System.out.println("[CRON JOB] health checking for ODM is finished");
    }

}
