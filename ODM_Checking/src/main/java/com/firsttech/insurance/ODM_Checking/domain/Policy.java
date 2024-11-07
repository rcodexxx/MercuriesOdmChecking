package com.firsttech.insurance.ODM_Checking.domain;

import java.util.Date;

public class Policy {
	public Date keep_date_time;
	private String policy_no;
	private String case_in;

	public String getPolicy_no() {
		return policy_no;
	}

	public String getCase_in() {
		return case_in;
	}

	public Policy(Date keep_date_time, String policy_no, String case_in) {
		this.keep_date_time = keep_date_time;
		this.policy_no = policy_no;
		this.case_in = case_in;
	}
}
