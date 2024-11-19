package com.firsttech.insurance.odmchecking.service;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firsttech.insurance.odmchecking.domain.Policy;
import com.firsttech.insurance.odmchecking.service.utils.DateUtil;
import com.firsttech.insurance.odmchecking.service.utils.FileUtil;
import com.firsttech.insurance.odmchecking.service.utils.HttpUtil;

@Service
public class VersionComparingService {

	private final static Logger logger = LoggerFactory.getLogger(VersionComparingService.class);
	
	@Autowired
	private Environment environment;
	private final String ODM8_CHECK_NB_URL_KEY = "odm8CheckNBUrl";
	private final String ODM9_CHECK_NB_URL_KEY = "odm9CheckNBUrl";
	private final String ODM8_CHECK_TA_URL_KEY = "odm8CheckTAUrl";
	private final String ODM9_CHECK_TA_URL_KEY = "odm9CheckTAUrl";
	private final String DB_SCHEMA_KEY = "dbSchema";
	private final String DB_URL_KEY = "dbUrl";
	private final String DB_USERNAME_KEY = "dbUsername";
	private final String DB_PASSWORD_KEY = "dbPassword";
	private final String ENV = "ENV";
	
	/**
	 * Main
	 * 1. 取得 properties 和 IPInfo.txt 資訊
	 * 2. 建立 Report Header
	 * 3. 建立 Report Body
	 * 4. 建立 Report Footer
	 * 5. 匯出 Report
	 * 
	 * @param startDate: yyyyMMdd
	 * @param endDate: yyyyMMdd
	 * @return
	 */
	public boolean doComparing (String startDate, String endDate) {
		// 1. 取得 properties 和 IPInfo.txt 資訊
		String infoFilePath = environment.getProperty("current.ip.info");
		Map<String, String> infoMap = FileUtil.getLocalIpInfo(infoFilePath);
		String currentIP = infoMap.get("local.ip");
		logger.info("取得當下IP: " + currentIP);
		Map<String, String> reqUrlMap = this.getODMRequestUrlMap(currentIP);
		Date today = new Date();
		
		// 2. 建立 Report Header
		List<String> rptTotalList = new ArrayList<>();
		rptTotalList.add("ODM Comparing Test Start from " + startDate + " to " + endDate);
		rptTotalList.add("");
		rptTotalList.add("Test IP: " + currentIP);
		rptTotalList.add("ODM CHECK ORIGINAL NB URL: " + reqUrlMap.get(ODM8_CHECK_NB_URL_KEY));
		rptTotalList.add("ODM CHECK NEW NB URL: " + reqUrlMap.get(ODM9_CHECK_NB_URL_KEY));
		rptTotalList.add("ODM CHECK ORIGINAL TA URL: " + reqUrlMap.get(ODM8_CHECK_TA_URL_KEY));
		rptTotalList.add("ODM CHECK NEW TA URL: " + reqUrlMap.get(ODM9_CHECK_TA_URL_KEY));
		rptTotalList.add("Test Date Time: " + DateUtil.formatDateToString("yyyy-MM-dd hh:mm:ss", today));
		rptTotalList.add("");
		rptTotalList.add("PolicyNo, Status, Diff");
		rptTotalList.add("======================================================");
		
		// 3. 建立 Report Body: Elvis說 ETS不需要測試, 只測NB和TA
		List<String> reportBody = new ArrayList<>();
		List<String> nbTestResult = this.calODM ("nb", reqUrlMap, startDate, endDate);
		List<String> taTestResult = this.calODM ("ta", reqUrlMap, startDate, endDate);
		
		reportBody.addAll(nbTestResult);
		reportBody.addAll(taTestResult);
		rptTotalList.addAll(reportBody);
		
		// 4. 建立 Report Footer
		rptTotalList.add("======================================================");
		int iPass = 0;
		int iFail = 0;
		int iError = 0;
		
		// 統計
		for (String eachRecord : reportBody) {
			if (eachRecord.contains("PASS")) {
				iPass += 1;
			} else if (eachRecord.contains("FAIL")) {
				iFail += 1;
			} else if (eachRecord.contains("ERROR")) {
				iError += 1;
			} 
		}
		
		rptTotalList.add("ODM Testing Result PASS: " + iPass + ", FAIL: " + iFail + ", ERROR: " + iError);
		
		// 5. 匯出 Report
		String rptOutputPath = environment.getProperty("output.path") 
				+ "\\ODM9_testing_report_" 
				+ reqUrlMap.get(ENV) + "_"
				+ DateUtil.formatDateToString("yyyyMMddhhmmss", today) 
				+ ".csv";
		logger.info("匯出報告路徑: {}", rptOutputPath);
		boolean isSuccess = FileUtil.writeToFile(rptTotalList, rptOutputPath);
		logger.info("比對報告產生結果: " + (isSuccess ? "SUCCESSFUL" : "FAIL"));
		return isSuccess;
		
	}
	
	/**
	 * 依照現行IP來取得 ODM 要測試新舊機器的URL連結
	 * @param currentIP: 當前機器的 IP
	 * @return key
	 * 		ODM8_CHECK_NB_URL_KEY
	 * 		ODM9_CHECK_NB_URL_KEY
	 * 		ODM8_CHECK_TA_URL_KEY
	 * 		ODM9_CHECK_TA_URL_KEY
	 * 		ENV: sit, uat, prod1, prod2
	 */
	private Map<String, String> getODMRequestUrlMap(String currentIP) {
		Map<String, String> map = new HashMap<>();
		// SIT
		if (currentIP.startsWith("172.16.16")) {
			map.put(ODM8_CHECK_NB_URL_KEY, environment.getProperty("odm.sit.nb.origin"));
			map.put(ODM9_CHECK_NB_URL_KEY, environment.getProperty("odm.sit.nb.new"));
			map.put(ODM8_CHECK_TA_URL_KEY, environment.getProperty("odm.sit.ta.origin"));
			map.put(ODM9_CHECK_TA_URL_KEY, environment.getProperty("odm.sit.ta.new"));
			map.put(ENV, "sit");
			map.put(DB_SCHEMA_KEY, "SITODMDB");
			map.put(DB_URL_KEY, environment.getProperty("db.sit.url"));
			map.put(DB_USERNAME_KEY, environment.getProperty("db.sit.username"));
			map.put(DB_PASSWORD_KEY, environment.getProperty("db.sit.password"));
		// UAT
		} else if (currentIP.startsWith("172.16.18")) {
			map.put(ODM8_CHECK_NB_URL_KEY, environment.getProperty("odm.uat.nb.origin"));
			map.put(ODM9_CHECK_NB_URL_KEY, environment.getProperty("odm.uat.nb.new"));
			map.put(ODM8_CHECK_TA_URL_KEY, environment.getProperty("odm.uat.ta.origin"));
			map.put(ODM9_CHECK_TA_URL_KEY, environment.getProperty("odm.uat.ta.new"));
			map.put(ENV, "uat");
			map.put(DB_SCHEMA_KEY, "UATODMDB");
			map.put(DB_URL_KEY, environment.getProperty("db.uat.url"));
			map.put(DB_USERNAME_KEY, environment.getProperty("db.uat.username"));
			map.put(DB_PASSWORD_KEY, environment.getProperty("db.uat.password"));
		// PROD1
		} else if (currentIP.equals("172.16.9.92")) {
			map.put(ODM8_CHECK_NB_URL_KEY, environment.getProperty("odm.prod1.nb.origin"));
			map.put(ODM9_CHECK_NB_URL_KEY, environment.getProperty("odm.prod1.nb.new"));
			map.put(ODM8_CHECK_TA_URL_KEY, environment.getProperty("odm.prod1.ta.origin"));
			map.put(ODM9_CHECK_TA_URL_KEY, environment.getProperty("odm.prod1.ta.new"));
			map.put(ENV, "prod1");
			map.put(DB_SCHEMA_KEY, "PRODODMDB");
			map.put(DB_URL_KEY, environment.getProperty("db.prod.url"));
			map.put(DB_USERNAME_KEY, environment.getProperty("db.prod.username"));
			map.put(DB_PASSWORD_KEY, environment.getProperty("db.prod.password"));
		// PROD2
		} else if (currentIP.equals("172.16.9.93")) {
			map.put(ODM8_CHECK_NB_URL_KEY, environment.getProperty("odm.prod2.nb.origin"));
			map.put(ODM9_CHECK_NB_URL_KEY, environment.getProperty("odm.prod2.nb.new"));
			map.put(ODM8_CHECK_TA_URL_KEY, environment.getProperty("odm.prod2.ta.origin"));
			map.put(ODM9_CHECK_TA_URL_KEY, environment.getProperty("odm.prod2.ta.new"));
			map.put(ENV, "prod2");
			map.put(DB_SCHEMA_KEY, "PRODODMDB");
			map.put(DB_URL_KEY, environment.getProperty("db.prod.url"));
			map.put(DB_USERNAME_KEY, environment.getProperty("db.prod.username"));
			map.put(DB_PASSWORD_KEY, environment.getProperty("db.prod.password"));
		} else {
			logger.info("沒有找到本機IP資訊無法對應到正確的 ODM URL");
		}

		return map;
	}
	
	/**
	 * a. DB 取得驗測案例 IN
	 * b. DB 取得驗測案例 OUT
	 * c. 準備 request header
	 * d. 讀取今日測試IN案例
	 * e. 找到對應的OUT結果JSON資料
	 * f. 呼叫 升級後 ODM9
	 * g. 取得 OUT和response 的 核保碼 (noteCode)列表
	 * h. 開始比對
	 * i. 組合報告body 加入report list
	 * 
	 * @param target: nb | ta
	 * @param startDate: yyyyMMdd
	 * @param endDate: yyyyMMdd
	 * @return
	 */
	private List<String> calODM (String target, Map<String, String> reqUrlMap, String startDate, String endDate) {
		logger.info("---------------------------------------------------------");
		logger.info("開始比對目標: {} ", target);
		List<String> bodyList = new ArrayList<>();
		
		// a. DB 取得驗測案例 IN
		String caseInTableName = target.equals("nb") ? "nb_case_in" : "ta_case_in";
		String caseInColumnName = target.equals("nb") ? "nb_json_in" : "ta_json_in";
        String caseInSql = this.getTestDataSQL(caseInTableName, caseInColumnName, startDate, endDate);
        List<Policy> caseInList = this.getCaseFromDB(target, "in", caseInSql, reqUrlMap);
        int CaseInNum = caseInList.size();
		logger.info("DB CaseIn 取出資料總比數為: {}" + CaseInNum);
		
		// b. DB 取得驗測案例 OUT
		String caseOutTableName = target.equals("nb") ? "nb_case_out" : "ta_case_out";
		String caseOutColumnName = target.equals("nb") ? "nb_json_out" : "ta_json_out";
        String caseOutSql = this.getTestDataSQL(caseOutTableName, caseOutColumnName, startDate, endDate);
        List<Policy> caseOutList = this.getCaseFromDB(target, "out", caseOutSql, reqUrlMap);
        int CaseOutNum = caseOutList.size();
		logger.info("DB CaseOut 取出資料總比數為: {}" + CaseOutNum);
		
		if (CaseInNum != CaseOutNum) {
			logger.info("比對目標 CaseIn 與 CaseOut 數量不符, CaseIn: {} and CaseOut: {}", CaseInNum, CaseOutNum);
		}
		
		// c. 準備 request header
		Map<String, String> headerMap = new HashMap<>();
		headerMap.put("Accept", "application/json");
		headerMap.put("Content-type", "application/json");
//		String odm8CheckUrl = target.equals("nb") ? reqUrlMap.get(ODM8_CHECK_NB_URL_KEY) : reqUrlMap.get(ODM8_CHECK_TA_URL_KEY);
		String odm9CheckUrl = target.equals("nb") ? reqUrlMap.get(ODM9_CHECK_NB_URL_KEY) : reqUrlMap.get(ODM9_CHECK_TA_URL_KEY);
		HttpUtil httpUtil = new HttpUtil();
		ObjectMapper mapper = new ObjectMapper();
		
		// d. 讀取今日測試IN案例
		for (Policy policy : caseInList) {
			
			// 20241118 modify by Peter : 改讀取 caseOut table
			// 	原本的設計沒有 caseIn 對應 到 caseOut的 key, 已詢問 Elvis 就按照時間順序
			// 	in 的第一筆對應到 out找到的第一筆即可
			// e. 找到對應的OUT結果JSON資料
			Optional<Policy> caseOutPolicy = caseOutList.stream()
                    .filter(outPolicy -> outPolicy.getMappingKey().equals(policy.getMappingKey()))
                    .findFirst();
			String odm8ResponseContent = null;
			if (caseOutPolicy.isPresent()) {
				odm8ResponseContent = caseOutPolicy.get().getJsonStr();
			} else {
				logger.info("無法在 caseOut 找到對應資料: {}", policy.toString());
			}
			
//			String odm8ResponseContent = null;
//			// 呼叫 現行 ODM8
//			try {
//				HttpResponse originResponse = httpUtil.httpRequestPost(odm8CheckUrl, policy.getJsonStr(), headerMap);
//				int statusCode = originResponse.getStatusLine().getStatusCode();
//				odm8ResponseContent = EntityUtils.toString(originResponse.getEntity(), "UTF-8");
//				if (statusCode >= 200 && statusCode < 300) {
//					logger.info("odm8 SUCCESS with policyNo: {}, status code: {}", policy.getPolicy_no(), statusCode);
//				} else {
//					logger.info("odm8 FAIL with policyNo: {}, status code: {}, return body: {}", policy.getPolicy_no(), statusCode,  odm8ResponseContent);
//				}
//			} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
//				e.printStackTrace();
//			}
			
			String odm9ResponseContent = null;
			// f. 呼叫 升級後 ODM9
			try {
				HttpResponse originResponse = httpUtil.httpRequestPost(odm9CheckUrl, policy.getJsonStr(), headerMap);
				int statusCode = originResponse.getStatusLine().getStatusCode();
				odm9ResponseContent = EntityUtils.toString(originResponse.getEntity(), "UTF-8");
				if (statusCode >= 200 && statusCode < 300) {
					logger.info("odm9 SUCCESS with policyNo: {}, status code: {}", policy.getPolicy_no(), statusCode);
				} else {
					logger.info("odm9 FAIL with policyNo: {}, status code: {}, return body: {}", policy.getPolicy_no(), statusCode,  odm9ResponseContent);
				}
			} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				e.printStackTrace();
			}
			
			// g. 取得 response 的 核保碼 (noteCode)列表
			List<String> nodeCode8 = null;
			List<String> nodeCode9 = null;
			try {
				
				if (target.equals("nb")) {
        			nodeCode8 = mapper.readTree(odm8ResponseContent).path("outParam").path("resultItem").findValuesAsText("noteCode");
        			nodeCode9 = mapper.readTree(odm9ResponseContent).path("outParam").path("resultItem").findValuesAsText("noteCode");
        		} else {
        			nodeCode8 = mapper.readTree(odm8ResponseContent).path("VerifyResult").path("resultItem").findValuesAsText("noteCode");
        			nodeCode9 = mapper.readTree(odm9ResponseContent).path("VerifyResult").path("resultItem").findValuesAsText("noteCode");
        		}
				
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			
			// h. 開始比對
			StringBuilder eachRowSb = new StringBuilder();
			String status = "";
	    	String diff = "";
	    	
	    	if (nodeCode8.isEmpty() || nodeCode8 == null) {
	    		logger.info("nodeCode8 is empty or null, odm8ResponseContent: {}", odm8ResponseContent);
	    		status = "ERROR";
	    		diff = "Origin 發生錯誤.";
			} else if (nodeCode9.isEmpty() || nodeCode9 == null) {
				logger.info("nodeCode9 is empty or null, odm9ResponseContent: {}", odm9ResponseContent);
				status = "ERROR";
				diff = "new 發生錯誤.";
			} else {
				if (this.isEqual(nodeCode8, nodeCode9)) {
					status = "PASS";
					diff ="NoteCode is same.";
				} else {
					status = "FAIL";
					diff = this.getDiffCodes(nodeCode8, nodeCode9);
				}
			}
			
	    	// i. 組合報告body 加入list
			eachRowSb.append(policy.getPolicy_no()).append(", ")
				  .append(status).append(", ")
				  .append(diff);
			
			bodyList.add(eachRowSb.toString());
		}
		
		return bodyList;
	}
	
	/**
	 * 因為caseIn和caseOut當初沒有設計對應key值, 所以SQL用trans_no和policy_no GROUP 對相同群組內的資料依據時間排序1,2,3
	 * @param tableName : nb_case_in | ta_case_in | nb_case_out | ta_case_out
	 * @param columnName: nb_json_in | ta_json_in | nb_json_out | ta_json_out
	 * @param startDate: yyyyMMdd
	 * @param endDate: yyyyMMdd
	 * @return
	 */
	private String getTestDataSQL(String tableName, String columnName, String startDate, String endDate) {
		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
		String startDateStr = LocalDate.parse(startDate, f).format(f).toString() + " 00:00:00";	
		String endDateStr =	LocalDate.parse(endDate, f).format(f).toString() + " 23:59:59";
		
		StringBuilder sqlSb = new StringBuilder();
		sqlSb.append(" SELECT ");
		sqlSb.append(" 		trans_no + policy_no + RowRank AS mappingKey, ");
		sqlSb.append(" 		trans_no, policy_no, keep_date_time, ").append(columnName);
		sqlSb.append(" FROM ( ");
		sqlSb.append(" 		SELECT ");
		sqlSb.append(" 			CAST(ROW_NUMBER() OVER (PARTITION BY trans_no, policy_no ORDER BY keep_date_time ASC) AS VARCHAR(5)) AS rowRank,");
		sqlSb.append(" 			trans_no, policy_no, keep_date_time, ").append(columnName);
		sqlSb.append(" 		FROM ").append(DB_SCHEMA_KEY).append(".dbo.").append(tableName);
		sqlSb.append(" 		WHERE keep_date_time BETWEEN '").append(startDateStr).append("' AND '").append(endDateStr).append("' ");
		sqlSb.append(" ) data ");
		sqlSb.append(" ORDER BY keep_date_time ");
		logger.info("Query DB SQL: {}", sqlSb.toString());
		return sqlSb.toString();
	}
	
	// 取得 caseIn or caseOut 資料
	private List<Policy> getCaseFromDB(String target, String inOut, String sql, Map<String, String> reqUrlMap) {
		String url = reqUrlMap.get(DB_URL_KEY);
		String username = reqUrlMap.get(DB_USERNAME_KEY);
		String password = reqUrlMap.get(DB_PASSWORD_KEY);

		logger.info("DB connection info: url: {}", url);

		List<Policy> list = new ArrayList<>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			// 建立連線
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			conn = DriverManager.getConnection(url, username, password);
			stmt = conn.createStatement(); // 建立 Statement 物件
			rs = stmt.executeQuery(sql); // 執行查詢
			Policy policy = null;

			// 處理查詢結果
			while (rs.next()) {
				policy = new Policy();
				policy.setMappingKey(rs.getString("mappingKey"));
				policy.setTrans_no(rs.getString("trans_no"));
				policy.setPolicy_no(rs.getString("policy_no"));
				policy.setKeep_date_time(rs.getDate("keep_date_time"));
				policy.setJsonStr(rs.getString(target + "_json_" + inOut));
				list.add(policy);
			}

		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				stmt.close();
				conn.close();
			} catch (Exception e) {
				logger.debug("資源關閉...");
			}

		}

		return list;
	}
	
	private boolean isEqual(List<String> nodeCode1, List<String> nodeCode2) {
		if (nodeCode1.equals(nodeCode2))
			return true;
		return false;
	}

	private String getDiffCodes(List<String> nodeCode1, List<String> nodeCode2) {
		StringBuilder sb = new StringBuilder();
		for (String code1 : nodeCode1) {
			boolean isDuplicated = false;
			for (String code2 : nodeCode2) {
				if (code1.equals(code2)) {
					isDuplicated = true;
					break;
				}
			}

			if (isDuplicated == false) {
				sb.append("[少] " + code1).append("; ");
			}
		}

		for (String code2 : nodeCode2) {
			boolean isDuplicated = false;
			for (String code1 : nodeCode1) {
				if (code2.equals(code1)) {
					isDuplicated = true;
					break;
				}
			}

			if (isDuplicated == false) {
				sb.append("[多] " + code2).append("; ");
			}
		}

		return sb.toString();
	}
}
