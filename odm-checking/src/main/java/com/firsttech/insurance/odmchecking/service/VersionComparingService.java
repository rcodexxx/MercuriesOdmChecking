package com.firsttech.insurance.odmchecking.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firsttech.insurance.odmchecking.domain.Policy;
import com.firsttech.insurance.odmchecking.service.utils.DateUtil;
import com.firsttech.insurance.odmchecking.service.utils.FileUtil;
import com.firsttech.insurance.odmchecking.service.utils.HttpUtil;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Date;
import java.util.*;

@Service
public class VersionComparingService {

    private final static Logger logger = LoggerFactory.getLogger(VersionComparingService.class);
    private final HttpUtil httpUtil = new HttpUtil();
    private final String ODM8_CHECK_NB_URL_KEY = "odm8CheckNBUrl";
    private final String ODM9_CHECK_NB_URL_KEY = "odm9CheckNBUrl";
    private final String ODM8_CHECK_TA_URL_KEY = "odm8CheckTAUrl";
    private final String ODM9_CHECK_TA_URL_KEY = "odm9CheckTAUrl";
    private final String ODM8_CHECK_ETS_URL_KEY = "odm8CheckETSUrl";
	private final String ODM9_CHECK_ETS_URL_KEY = "odm9CheckETSUrl";
    private final String DB_SCHEMA_KEY = "dbSchema";
    private final String DB_URL_KEY = "dbUrl";
    private final String DB_USERNAME_KEY = "dbUsername";
    private final String DB_PASSWORD_KEY = "dbPassword";
    private final String ENV = "ENV";
    @Autowired
    private Environment environment;

    /**
     * Main 1. 取得 properties 和 IPInfo.txt 資訊 2. 建立 Report Header 3. 建立 Report Body
     * 比對新舊版本 ODM 結果 4. 建立 Report Footer 統計案件結果 5. 匯出 Report
     *
     * @param startDateTime: 民國年月日時分秒 yyyMMddhhmmss
     * @param endDateTime:   民國年月日時分秒 yyyMMddhhmmss
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public boolean doComparing(String startDateTime, String endDateTime) {
        // 1. 取得 properties 和 IPInfo.txt 資訊
        String infoFilePath = environment.getProperty("current.ip.info");
        Map<String, String> infoMap = FileUtil.getLocalIpInfo(infoFilePath);
        String currentIP = infoMap.get("local.ip");
        logger.info("取得當下IP: " + currentIP);
        Map<String, String> reqUrlMap = this.getODMRequestUrlMap(currentIP);

        // 2. 建立 Report Header
        List<String> rptTotalList = new ArrayList<>();
        rptTotalList.add("ODM Comparing Test Start from " + startDateTime + " to " + endDateTime);
        rptTotalList.add("");
        rptTotalList.add("Test IP: " + currentIP);
        rptTotalList.add("ODM CHECK ORIGINAL NB URL: " + reqUrlMap.get(ODM8_CHECK_NB_URL_KEY));
        rptTotalList.add("ODM CHECK NEW NB URL: " + reqUrlMap.get(ODM9_CHECK_NB_URL_KEY));
        rptTotalList.add("ODM CHECK ORIGINAL TA URL: " + reqUrlMap.get(ODM8_CHECK_TA_URL_KEY));
        rptTotalList.add("ODM CHECK NEW TA URL: " + reqUrlMap.get(ODM9_CHECK_TA_URL_KEY));
        rptTotalList.add("Test Date Time: " + DateUtil.formatDateToString("yyyy-MM-dd hh:mm:ss", new Date()));
        rptTotalList.add("");
        rptTotalList.add("TransNo, PolicyNo, KeepDateTime, Status, Diff, ODM 8 node code, ODM 9 node code");
        rptTotalList.add("=============================================================================================");

        // 3. 建立 Report Body: Elvis說 ETS不需要測試, 只測NB和TA
        List<String> reportBody = new ArrayList<>();
        List<String> nbTestResult = this.comparingNewOldByCaseIn("nb", reqUrlMap, startDateTime, endDateTime);
        List<String> taTestResult = this.comparingNewOldByCaseIn("ta", reqUrlMap, startDateTime, endDateTime);

        reportBody.addAll(nbTestResult);
        reportBody.addAll(taTestResult);
        rptTotalList.addAll(reportBody);

        // 4. 建立 Report Footer
        rptTotalList.add("=============================================================================================");
        int iPass = 0;
        int iFail = 0;
        int iError = 0;

        // 統計
        for (String eachRecord : reportBody) {
            if (eachRecord.contains("PASS")) {
                iPass += 1;
            } else if (eachRecord.contains("FAIL")) {
                iFail += 1;
            } else {
                iError += 1;
            }
        }

        rptTotalList.add("ODM Testing Result PASS: " + iPass + ", FAIL: " + iFail + ", OTHER: " + iError);

        // 5. 匯出 Report
        String rptOutputPath = environment.getProperty("output.path") + "\\ODM9_testing_report_" + reqUrlMap.get(ENV)
                + "_" + endDateTime + ".csv";
        logger.info("匯出報告路徑: {}", rptOutputPath);
        boolean isSuccess = FileUtil.writeToFile(rptTotalList, rptOutputPath);
        logger.info("比對報告產生結果: " + (isSuccess ? "SUCCESSFUL" : "FAIL"));
        return isSuccess;
    }

    /**
     * 依照現行IP來取得 ODM 要測試新舊機器的URL連結
     *
     * @param currentIP: 當前機器的 IP
     * @return keys ODM8_CHECK_NB_URL_KEY | ODM9_CHECK_NB_URL_KEY
     * ODM8_CHECK_TA_URL_KEY | ODM9_CHECK_TA_URL_KEY DB_SCHEMA_KEY |
     * DB_URL_KEY | DB_USERNAME_KEY | DB_PASSWORD_KEY ENV: sit, uat, prod1,
     * prod2
     */
    private Map<String, String> getODMRequestUrlMap(String currentIP) {
        Map<String, String> map = new HashMap<>();
        // SIT
        if (currentIP.startsWith("172.16.16")) {
            map.put(ODM8_CHECK_NB_URL_KEY, environment.getProperty("odm.sit.nb.origin"));
            map.put(ODM9_CHECK_NB_URL_KEY, environment.getProperty("odm.sit.nb.new"));
            map.put(ODM8_CHECK_TA_URL_KEY, environment.getProperty("odm.sit.ta.origin"));
            map.put(ODM9_CHECK_TA_URL_KEY, environment.getProperty("odm.sit.ta.new"));
            map.put(ODM8_CHECK_ETS_URL_KEY, environment.getProperty("odm.sit.ets.origin"));
			map.put(ODM9_CHECK_ETS_URL_KEY, environment.getProperty("odm.sit.ets.new"));
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
            map.put(ODM8_CHECK_ETS_URL_KEY, environment.getProperty("odm.uat.ets.origin"));
			map.put(ODM9_CHECK_ETS_URL_KEY, environment.getProperty("odm.uat.ets.new"));
            map.put(ENV, "uat");
            map.put(DB_SCHEMA_KEY, "UATODMDB");
            map.put(DB_URL_KEY, environment.getProperty("db.uat.url"));
            map.put(DB_USERNAME_KEY, environment.getProperty("db.uat.username"));
            map.put(DB_PASSWORD_KEY, environment.getProperty("db.uat.password"));
            // PROD1
        } else if (currentIP.startsWith("172.16.9")) {
            map.put(ODM8_CHECK_NB_URL_KEY, environment.getProperty("odm.prod1.nb.origin"));
            map.put(ODM9_CHECK_NB_URL_KEY, environment.getProperty("odm.prod1.nb.new"));
            map.put(ODM8_CHECK_TA_URL_KEY, environment.getProperty("odm.prod1.ta.origin"));
            map.put(ODM9_CHECK_TA_URL_KEY, environment.getProperty("odm.prod1.ta.new"));
            map.put(ODM8_CHECK_ETS_URL_KEY, environment.getProperty("odm.prod1.ets.origin"));
			map.put(ODM9_CHECK_ETS_URL_KEY, environment.getProperty("odm.prod1.ets.new"));
            map.put(ENV, "prod1");
            map.put(DB_SCHEMA_KEY, "ODMDB");
            map.put(DB_URL_KEY, environment.getProperty("db.prod.url"));
            map.put(DB_USERNAME_KEY, environment.getProperty("db.prod.username"));
            map.put(DB_PASSWORD_KEY, environment.getProperty("db.prod.password"));
        } else {
            logger.info("沒有找到本機IP資訊無法對應到正確的 ODM URL");
        }

        return map;
    }

    /**
     * 呼叫 api
     *
     * @param odmCheckUrl
     * @param postBody
     * @param httpContext
     * @param headerMap
     * @return
     */
    private String callOdm(String odmCheckUrl, String postBody, HttpClientContext httpContext,
                           Map<String, String> headerMap) {
        CloseableHttpClient httpClient = null;
        HttpResponse odmResponse = null;
        String odmResponseContent = null;

        try {
            HttpPost request = new HttpPost(odmCheckUrl);
            for (String key : headerMap.keySet()) {
                request.setHeader(key, headerMap.get(key));
            }
            request.setEntity(new StringEntity(postBody, ContentType.APPLICATION_JSON));

            httpClient = httpUtil.getHttpClient();
            odmResponse = httpClient.execute(request, httpContext);
            int statusCode = odmResponse.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                odmResponseContent = EntityUtils.toString(odmResponse.getEntity(), "UTF-8");
//	        	logger.info("call ODM success with 200, odmResponseContent: {}", odmResponseContent);
            } else {
                logger.info("呼叫ODM發生錯誤 回傳 status code: {}", statusCode);
            }

        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
            logger.info("呼叫 ODM 發生錯誤: {}", e.getMessage());
        } finally {
            try {
                assert httpClient != null;
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return odmResponseContent;
    }

    /**
     * 3. 建立 Report Body 比對新舊版本 ODM 結果 a. DB 取得驗測案例 IN b. DB 取得驗測案例 OUT c. 準備 api
     * 發送和比對所需資料 d. 逐筆讀取今日測試IN案例 e. 呼叫 升級前 ODM8 或 正式環境找對應的caseOut json f. 呼叫 升級後
     * ODM9 g. 取得 responseContent 的 核保碼 (noteCode)列表 h. 開始比對 i. 組合報告body 加入report
     * list
     *
     * @param target:        nb | ta
     * @param startDateTime: 民國年月日時分秒 yyyMMddhhmmss
     * @param endDateTime:   民國年月日時分秒 yyyMMddhhmmss
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private List<String> comparingNewOldByCaseIn(String target, Map<String, String> reqUrlMap, String startDateTime,
                                                 String endDateTime) {
        logger.info("---------------------------------------------------------");
        logger.info("開始比對目標: {} ", target);
        List<String> bodyList = new ArrayList<>();

        // a. DB 取得驗測案例 IN
        List<Policy> caseInList = this.getCaseInDataFromDB(target, startDateTime, endDateTime, reqUrlMap);

        // b. DB 取得驗測案例 OUT
        List<Policy> caseOutList = this.getCaseOutDataFromDB(target, startDateTime, endDateTime, reqUrlMap);

        int CaseInNum = caseInList.size();
        int CaseOutNum = caseOutList.size();
        if (CaseInNum != CaseOutNum) {
            logger.info("比對目標 CaseIn 與 CaseOut 數量不符, CaseIn: {} and CaseOut: {}", CaseInNum, CaseOutNum);
        } else {
            logger.info("比對目標 CaseIn 與 CaseOut 數量相符, CaseIn: {} and CaseOut: {}", CaseInNum, CaseOutNum);
        }

        // c. 準備 api 發送和比對所需資料
        // request header
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Accept", "application/json");
        headerMap.put("Content-type", "application/json");

        // odm url
        String odm8CheckUrl = target.equals("nb") ? reqUrlMap.get(ODM8_CHECK_NB_URL_KEY)
                : reqUrlMap.get(ODM8_CHECK_TA_URL_KEY);
        String odm9CheckUrl = target.equals("nb") ? reqUrlMap.get(ODM9_CHECK_NB_URL_KEY)
                : reqUrlMap.get(ODM9_CHECK_TA_URL_KEY);

        // 參數宣告
        StringBuilder eachRowSb = null;
        Policy caseOutPolicy = null;
        List<String> nodeCode8 = null;
        List<String> nodeCode9 = null;
        String currentEnv = reqUrlMap.get(ENV);
        HttpClientContext httpContext = HttpClientContext.create();

        // d. 讀取今日測試IN案例
        for (Policy policy : caseInList) {
//            logger.info("===> " + policy.toStringWithoutJson());
            eachRowSb = new StringBuilder();

            // e. 呼叫 升級前 ODM8 或 正式環境找對應的caseOut json
            String odm8ResponseContent = null;
            if (currentEnv.equals("prod1") || currentEnv.equals("prod2")) {
//                caseOutPolicy = caseOutMap.get(policy.getMappingKey());
            	caseOutPolicy = this.getCaseOutPolicy(policy, caseOutList);
                if (caseOutPolicy != null) {
                    odm8ResponseContent = caseOutPolicy.getJsonStr();
                } else {
                    eachRowSb.append("DB caseOut no output result");
                    bodyList.add(eachRowSb.toString());
                    logger.info("DB caseOut no output result, {}", policy);
                    continue;
                }
            } else {
                odm8ResponseContent = this.callOdm(odm8CheckUrl, policy.getJsonStr(), httpContext, headerMap);
                if (odm8ResponseContent == null) {
                    bodyList.add("呼叫 ODM 8 發生錯誤");
                    logger.info("呼叫 ODM 8 發生錯誤, json: {}", policy);
                    continue;
                }
            }

            // f. 呼叫 升級後 ODM9
            String odm9ResponseContent = this.callOdm(odm9CheckUrl, policy.getJsonStr(), httpContext, headerMap);
            if (odm9ResponseContent == null) {
                bodyList.add("呼叫 ODM 9 發生錯誤");
                logger.info("呼叫 ODM 9 發生錯誤, {}", policy);
                continue;
            }

            // g. 取得 responseContent 的 核保碼 (noteCode)列表
            String[] nbJsonNode = {"outParam", "resultItem", "noteCode"};
            String[] taJsonNode = {"VerifyResult", "resultItem", "noteCode"};
            if (target.equals("nb")) {
                nodeCode8 = getNodeCode(odm8ResponseContent, nbJsonNode);
                nodeCode9 = getNodeCode(odm9ResponseContent, nbJsonNode);
            } else {
                nodeCode8 = getNodeCode(odm8ResponseContent, taJsonNode);
                nodeCode9 = getNodeCode(odm9ResponseContent, taJsonNode);
            }

            logger.info("Result: ODM8 node code: {}, ODM9 node code: {}", nodeCode8.toString(), nodeCode9.toString());

            // h. 開始比對
            String status = null;
            String diff = null;

            if (nodeCode8.isEmpty() && nodeCode9.isEmpty()) {
                status = "PASS";
                diff = "Both are No NoteCode.";
            } else if (nodeCode8.isEmpty()) {
                status = "FAIL";
                diff = "ODM 8 has no nodeCode.";
            } else if (nodeCode9.isEmpty()) {
                status = "FAIL";
                diff = "ODM 9 has no nodeCode.";
            } else if (nodeCode8.equals(nodeCode9)) {
                status = "PASS";
                diff = "NoteCode is same.";
            } else {
                status = "FAIL";
                diff = this.getDiffCodes(nodeCode8, nodeCode9);
            }

            // i. 組合報告body 加入list
            String keepDateTime = DateUtil.formatDateToString("yyyy-MM-dd hh:mm:ss", policy.getKeep_date_time());
            eachRowSb.append(policy.getTrans_no()).append(", ")
                    .append(policy.getPolicy_no()).append(", ")
                    .append(keepDateTime).append(", ")
                    .append(status).append(", ")
                    .append(diff).append(", ")
                    .append(nodeCode8.toString()).append(", ")
                    .append(nodeCode9.toString()).append(", ");
            bodyList.add(eachRowSb.toString());
        }

        return bodyList;
    }

    /**
     * a. DB 取得驗測案例 IN
     *
     * @param target:        nb | ta
     * @param startDateTime: 民國年月日時分秒 (yyyMMddhhmmss)
     * @param endDateTime:   民國年月日時分秒 (yyyMMddhhmmss)
     * @param reqUrlMap
     * @return
     */
    private List<Policy> getCaseInDataFromDB(String target, String startDateTime, String endDateTime,
                                             Map<String, String> reqUrlMap) {
        String caseInTableName = target.equals("nb") ? "nb_case_in" : "ta_case_in";
        String caseInColumnName = target.equals("nb") ? "nb_json_in" : "ta_json_in";

        String caseInSql = this.getTestDataSQL(reqUrlMap.get(DB_SCHEMA_KEY), caseInTableName, caseInColumnName,
                startDateTime, endDateTime);

        List<Policy> caseInList = this.getCaseFromDB(target, "in", caseInSql, reqUrlMap);
        return caseInList;
    }

    /**
     * b. DB 取得驗測案例 OUT
     *
     * @param target:        nb | ta
     * @param startDateTime: 民國年月日時分秒 (yyyMMddhhmmss)
     * @param endDateTime:   民國年月日時分秒 (yyyMMddhhmmss)
     * @param reqUrlMap
     * @return
     */
    public List<Policy> getCaseOutDataFromDB(String target, String startDateTime, String endDateTime,
                                             Map<String, String> reqUrlMap) {
        String caseOutTableName = target.equals("nb") ? "nb_case_out" : "ta_case_out";
        String caseOutColumnName = target.equals("nb") ? "nb_json_out" : "ta_json_out";
        String caseOutSql = this.getTestDataSQL(reqUrlMap.get(DB_SCHEMA_KEY), caseOutTableName, caseOutColumnName,
                startDateTime, endDateTime);
        return this.getCaseFromDB(target, "out", caseOutSql, reqUrlMap);
    }

    /**
     * 因為caseIn和caseOut當初沒有設計對應key值, 所以SQL用trans_no和policy_no GROUP
     * 對相同群組內的資料依據時間排序1,2,3
     *
     * @param tableName      : nb_case_in | ta_case_in | nb_case_out | ta_case_out
     * @param columnName:    nb_json_in | ta_json_in | nb_json_out | ta_json_out
     * @param startDateTime: 民國年月日時分秒 yyyMMddhhmmss
     * @param endDateTime    :    民國年月日時分秒 yyyMMddhhmmss
     * @return SQL DB
     */
    private String getTestDataSQL(String schemaName, String tableName, String columnName, String startDateTime,
                                  String endDateTime) {

        StringBuilder sqlSb = new StringBuilder();
        sqlSb.append(" SELECT ");
        sqlSb.append(" 		trans_no + policy_no + RowRank AS mappingKey, ");
        sqlSb.append(" 		transDateTime, trans_no, policy_no, keep_date_time, ").append(columnName);
        sqlSb.append(" FROM ( ");
        sqlSb.append(" 		SELECT ");
        sqlSb.append(" 			CAST(ROW_NUMBER() OVER (PARTITION BY trans_no, policy_no ORDER BY keep_date_time ASC) AS VARCHAR(5)) AS rowRank,");
        sqlSb.append(" 			SUBSTRING(trans_no, 1, 13) AS transDateTime, ");
        sqlSb.append(" 			trans_no, policy_no, keep_date_time, ").append(columnName);
        sqlSb.append(" 		FROM ").append(schemaName).append(".dbo.").append(tableName);
        sqlSb.append(" ) data ");
        sqlSb.append(" WHERE transDateTime BETWEEN '").append(startDateTime).append("' AND '").append(endDateTime).append("' ");
        sqlSb.append(" ORDER BY transDateTime ");

        logger.info("Query DB SQL: {}", sqlSb);
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

    private String getDiffCodes(List<String> originCodeList, List<String> newCodeList) {
        Map<String, Integer> map = new HashMap<>();
        for (String newCode : newCodeList) {
            if (map.containsKey(newCode)) {
                map.put(newCode, map.get(newCode) + 1);
            } else {
                map.put(newCode, 1);
            }
        }

        for (String originCode : originCodeList) {
            if (map.containsKey(originCode)) {
                map.put(originCode, map.get(originCode) - 1);
            } else {
                map.put(originCode, -1);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String key : map.keySet()) {
            int times = map.get(key);
            if (times == 0) {
                continue;
            } else if (times > 0) {
                sb.append("[多]").append(key).append("; ");
            } else {
                sb.append("[少]").append(key).append("; ");
            }

        }

        return sb.toString();
    }

    private List<String> getNodeCode(String targetJson, String[] jsonNodeValue) {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        List<String> nodeCode = new ArrayList<>();

        try (JsonParser parser = factory.createParser(targetJson)) {
            String currName = null;
            boolean inOutParam = false;
            boolean inResultItem = false;

            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) {
                    break;
                }

                switch (token) {
                    case FIELD_NAME:
                        currName = parser.getCurrentName();
                        break;

                    case START_OBJECT:
                        if (jsonNodeValue[0].equals(currName)) {
                            inOutParam = true;
                        } else if (inOutParam && jsonNodeValue[1].equals(currName)) {
                            inResultItem = true;
                        }
                        break;

                    case END_OBJECT:
                        if (inResultItem) {
                            inResultItem = false;
                        } else if (inOutParam) {
                            inOutParam = false;
                        }
                        break;

                    case VALUE_STRING:
                        if (inResultItem && jsonNodeValue[2].equals(currName)) {
                            nodeCode.add(parser.getValueAsString());
                        }
                        break;

                    default:
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return nodeCode;
    }

    
    public boolean doETSComparing() {
    	// 取得 ETS 要測試使用的檔案位置
    	String etsCsvPath = environment.getProperty("ets.csv.path");
 		if (etsCsvPath == null || etsCsvPath.equals("")) {
 			return false;
 		}
 		
 		HttpClientContext httpContext = HttpClientContext.create();
 		String infoFilePath = environment.getProperty("current.ip.info");
 		Map<String, String> infoMap = FileUtil.getLocalIpInfo(infoFilePath);
 		String currentIP = infoMap.get("local.ip");
 		logger.info("取得當下IP: " + currentIP);
 		
 		// 取得新舊 ETS ODM 呼叫網址
 		Map<String, String> reqUrlMap = this.getODMRequestUrlMap(currentIP);
 		String odm8CheckUrl = reqUrlMap.get(ODM8_CHECK_ETS_URL_KEY);
 		String odm9CheckUrl = reqUrlMap.get(ODM9_CHECK_ETS_URL_KEY); 
 		
 		// request header
 		Map<String, String> headerMap = new HashMap<>();
 		headerMap.put("Accept", "application/json");
 		headerMap.put("Content-type", "application/json");
 		
 		List<String[]> headerRptList = new ArrayList<>();
 		List<String[]> bodyRptList = new ArrayList<>();
 		
 		String[] title1 = {"original ETS ODM:", odm8CheckUrl,"",""};
 		String[] title2 = {"new ETS ODM:", odm9CheckUrl,"",""};
 		String[] title3 = {"pKey", "remark1", "remark2", "isMatch"};
 		headerRptList.add(title1);
 		headerRptList.add(title2);
 		headerRptList.add(title3);
 		
 		// 匯出報告
 	 	String defaultOutputPath = environment.getProperty("output.path");
 	 	String currentYMDHMS = DateUtil.formatDateToString("yyyyMMddhhmmss", new Date());
 	 	String rptOutputPath = defaultOutputPath + "\\ODM9_ets_report_" + reqUrlMap.get(ENV)
 	 		+ "_" + currentYMDHMS + ".csv";
 		
 		// 逐行讀取 CSV
 		try (CSVReader reader = new CSVReader(new FileReader(etsCsvPath))) {
            String[] nextLine;
            String[] thisLine;

			while ((nextLine = reader.readNext()) != null) {
				thisLine = new String[7];
				
				String pKey = nextLine[0] == null ? "" : nextLine[0];
				String jsonIn = nextLine[1] == null ? "" : nextLine[1];
				String remark1 = nextLine[2] == null ? "" : nextLine[2];
				String remark2 = nextLine[3] == null ? "" : nextLine[3];
				
				// 呼叫 origin odm
				String response8Content = this.callOdm(odm8CheckUrl, jsonIn, httpContext, headerMap);
	 			if (response8Content == null) {
	 				response8Content = "發生錯誤!!";
	 			}
	 			
	 			// 呼叫 new odm
	 			String response9Content = this.callOdm(odm9CheckUrl, jsonIn, httpContext, headerMap);
	 			if (response9Content == null) {
	 				response9Content = "發生錯誤!!";
	 			}
	 			
	 			response8Content = this.getOutParamFromODM(response8Content);
	 			response9Content = this.getOutParamFromODM(response9Content);
	 			
	 			thisLine[0] = pKey;
	 			thisLine[1] = remark1;
	 			thisLine[2] = remark2;
	 			thisLine[3] = response8Content.equals(response9Content) ? "相符" : "不相符";
	 			bodyRptList.add(thisLine);
	 			
	 			String jsonOutputOriginalPath = defaultOutputPath + "\\JSON_" + currentYMDHMS + "\\"
	 					+ pKey + "_origin.json";
	 			String jsonOutputNewPath = defaultOutputPath + "\\JSON_" + currentYMDHMS + "\\"
	 					+ pKey + "_new.json";
	 			
	 			FileUtil.writeToFile(Arrays.asList(response8Content), jsonOutputOriginalPath);
	 			FileUtil.writeToFile(Arrays.asList(response9Content), jsonOutputNewPath);
			}

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
 		
 		return this.writeDataToCSV(rptOutputPath, headerRptList, bodyRptList);
    }
    
    private boolean writeDataToCSV (String outputPath, List<String[]> headersList, List<String[]> bodysList) {
    	boolean isSuccess = false;
    	try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath))) {
            // 寫入 Header
    		for (String[] header : headersList) {
    			writer.writeNext(header);
    		}

            // 寫入資料列
    		for (String[] body : bodysList) {
    			writer.writeNext(body);
    		}

    		isSuccess = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    	return isSuccess;
    }
    
    private String getOutParamFromODM (String responseContent) {
    	ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode rootNode = objectMapper.readTree(responseContent);
			JsonNode outParamNode = rootNode.get("outParam");
			responseContent = objectMapper.writeValueAsString(outParamNode);
			
			// 處理CSV換行問題
			JSONObject jsonObject = new JSONObject(responseContent);
			responseContent = jsonObject.toString(); 
			
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return responseContent;
    }
     	
 	private Policy getCaseOutPolicy (Policy caseInPolicy, List<Policy> caseOutList) {
		Policy policy = null;
		for (Policy caseOutPolicy : caseOutList) {
			if (caseInPolicy.getTrans_no().equals(caseOutPolicy.getTrans_no()) 
					&& caseInPolicy.getPolicy_no().equals(caseOutPolicy.getPolicy_no())) {
				policy = caseOutPolicy;
				break;
			}
		}
		return policy;
	}
 	
 	
}