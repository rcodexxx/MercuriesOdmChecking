package com.firsttech.insurance.odmchecking.domain;

import java.util.Date;

public class Policy {
	public Date keep_date_time;
	private String mappingKey;
	private String trans_no;
	private String policy_no;
	private String jsonStr;

	public Date getKeep_date_time() {
		return keep_date_time;
	}
	public void setKeep_date_time(Date keep_date_time) {
		this.keep_date_time = keep_date_time;
	}
	public String getMappingKey() {
		return mappingKey;
	}
	public void setMappingKey(String mappingKey) {
		this.mappingKey = mappingKey;
	}
	public String getTrans_no() {
		return trans_no;
	}
	public void setTrans_no(String trans_no) {
		this.trans_no = trans_no;
	}
	public String getPolicy_no() {
		return policy_no;
	}
	public void setPolicy_no(String policy_no) {
		this.policy_no = policy_no;
	}
	public String getJsonStr() {
		return jsonStr;
	}
	public void setJsonStr(String jsonStr) {
		this.jsonStr = jsonStr;
	}
	public Policy () {
		
	}
	public Policy(Date keep_date_time, String policy_no, String jsonStr) {
		this.keep_date_time = keep_date_time;
		this.policy_no = policy_no;
		this.jsonStr = jsonStr;
	}
	
	public String toString() {
		return "mappingKey: " + mappingKey + ", "
				+ "trans_no: " + trans_no + ", "
				+ "policy_no: " + policy_no + ", "
				+ "keep_date_time: " + keep_date_time + ", "
				+ "jsonStr: " + jsonStr;
	}
	
	public String toStringWithoutJson() {
		return "mappingKey: " + mappingKey + ", "
				+ "trans_no: " + trans_no + ", "
				+ "policy_no: " + policy_no + ", "
				+ "keep_date_time: " + keep_date_time;
	}
}
