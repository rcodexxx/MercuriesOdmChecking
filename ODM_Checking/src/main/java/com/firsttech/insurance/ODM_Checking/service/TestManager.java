package com.firsttech.insurance.ODM_Checking.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.firsttech.insurance.ODM_Checking.domain.Policy;
import org.springframework.core.env.Environment;

public class TestManager {
	private Environment environment;
	private OdmTool odm = new OdmTool();
	private boolean testflag = false;
	private String env;
	private String url;
	private String user;
	private String pwd;
	private String date;
	private String fileName;
	public int pass = 0;
	public int fail = 0;
	public int error = 0;


	public TestManager(Environment environment) {
		this.environment = environment;
		this.testflag = environment.getProperty("testing") != null;
		this.env = environment.getProperty("env").toLowerCase();
		this.url = environment.getProperty("db.url");
		this.user = environment.getProperty("db.username");
		this.pwd = environment.getProperty("db.password");
		String filePath = environment.getProperty("output.path");
		this.fileName = filePath + "-" + this.env + "-" + getToday() + ".csv";
		this.date = getToday();
	}

	public void executeTest(Runnable test) {
		test.run();
	}
	
	public void createTest(String target, String startDate, String endDate) {
		String odmApi = environment.getProperty("odm." + target + ".origin");
		String odmApiNew = environment.getProperty("odm." + target + ".new");
		String status = null;
		int sPass = 0;
		int sFail = 0;
		int count = 0;
		List<Policy> policyList = new ArrayList<>();
		String q = setDateQuery(startDate, endDate, target);

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.fileName, true))) {
			try (Connection conn = connectDB(this.url, this.user, this.pwd);
					PreparedStatement ps = conn.prepareStatement(q)) {
				try (ResultSet rs = ps.executeQuery()) {
					mainLoop: while (rs.next()) {
						Date keep_data_time = rs.getDate("keep_date_time");
						String policy_no = rs.getString("policy_no");
						String json_in = rs.getString(target + "_json_in");

						Policy policy = new Policy(keep_data_time, policy_no, json_in);
						policyList.add(policy);
						if (!policyList.isEmpty()) {
							for (Policy p : policyList) {
								String policyNum = p.getPolicy_no();
								String policyInput = p.getCase_in();

								List<String> nodeCode8 = null;
								List<String> nodeCode9 = null;
								List<String> diff = new ArrayList<>();

								if ("nb".equals(target)) {
									nodeCode8 = odm.getNBNodeCode(odm.callODM(odmApi, policyInput));
									nodeCode9 = odm.getNBNodeCode(odm.callODM(odmApiNew, policyInput));
								} else if ("ta".equals(target)) {
									nodeCode8 = odm.getTANodeCode(odm.callODM(odmApi, policyInput));
									nodeCode9 = odm.getTANodeCode(odm.callODM(odmApiNew, policyInput));
								}

								if (nodeCode8.isEmpty() || nodeCode8 == null) {
									writer.write(target + " ODM8 connection error \n");
									this.error += 1;
									break mainLoop;
								} else if (nodeCode9.isEmpty() || nodeCode9 == null) {
									writer.write(target + " ODM9 connection error \n");
									this.error += 1;
									break mainLoop;
								}

								if (odm.isEqual(nodeCode8, nodeCode9)) {
									status = "PASS";
									diff.add("NoteCode is same.");
									this.pass += 1;
									sPass += 1;
								} else {
									status = "FAIL";
									diff = odm.getComparison(nodeCode8, nodeCode9);
									this.fail += 1;
									sFail += 1;
								}
								System.out.print(count + " | " + policyNum + " | " + status + " | " + diff + "\n");
								writer.write(count + " | " + policyNum + " | " + status + " | " + diff + "\n");
								count += 1;
							}
						}
					}
				}
				if (policyList.isEmpty()) {
					System.out.print("Today has no policy \n");
					writer.write("Today has no policy \n");
				} else if (this.error > 0) {
					writer.write("Test on " + target + " Error \n");
				} else {
					writer.write("Test on " + target + ", Pass = " + sPass + ", Fail = " + sFail + "\n");
				}
			} catch (SQLException e) {
				e.printStackTrace();
				writer.write("SQL server connection error \n");
				this.error += 1;
			}
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initTest() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.fileName, true))) {
			writer.write("ODM Testing Start\n");
			writer.write("Test date: " + getToday() + "\n");
			writer.write("Test start time: " + getTodayTime() + "\n");
			writer.write("Test Case ID | Policy num | Status | Comparison |\n");
			writer.write("=====================================================================================\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void closeTest() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.fileName, true))) {
			writer.write("=====================================================================================\n");
			writer.write("ODM test stop time: " + getTodayTime() + "\n");
			if (this.error > 0)
				writer.write("ODM Testing Result: Error \n");
			else
				writer.write("ODM Testing Result PASS: " + this.pass + ", FAIL: " + this.fail + "\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Connection connectDB(String url, String userName, String pwd) {
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			Connection conn = DriverManager.getConnection(url, userName, pwd);
			return conn;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.out.println("JDBC not found");
			return null;
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("fail to connection");
			return null;
		}
	}

	private String setDateQuery(String startDate, String endDate, String target) {
		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
		String startDateStr = startDate == null ?
				LocalDate.parse(this.date, f).format(f).toString() :
				LocalDate.parse(startDate, f).format(f).toString() + " 00:00:00";	
		String endDateStr = endDate == null ?
				LocalDate.parse(startDateStr, f).plusDays(1).format(f).toString() :
				LocalDate.parse(endDate, f).format(f).toString() + " 23:59:59";
		
		return "SELECT policy_no, keep_date_time, " + target + "_json_in " + "FROM SITODMDB.dbo." + target + "_case_in"
				+ " WHERE keep_date_time BETWEEN '" + startDateStr + "' AND '" + endDateStr + "'";
	}
	
//	private String setDateQuery(String date, String target) {
//		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
//		String tomorrow = LocalDate.parse(date, f).plusDays(1).format(f).toString();
//		return "SELECT policy_no, keep_date_time, " + target + "_json_in " + "FROM SITODMDB.dbo." + target + "_case_in"
//				+ " WHERE keep_date_time BETWEEN '" + date + "' AND '" + tomorrow + "'";
//	}

	public String getFileName() {
		return this.fileName;
	}

	public String getEnv() {
		return this.env;
	}

	public boolean getTestFlag() {
		return this.testflag;
	}
	
	private String getTodayTime() {
		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
		return LocalDateTime.now().format(f);
	}

	private String getToday() {
		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
		return LocalDate.now().format(f);
	}
}
