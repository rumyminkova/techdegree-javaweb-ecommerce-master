package com.acme.ecommerce.domain;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Pattern;

@Component
@Scope("session")
public class CouponCode {

	@Pattern(message="must be between 5 and 10 characters",regexp="^(\\s*|[0-9a-zA-Z]{5,10})$")

	private String code;
	
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
}
