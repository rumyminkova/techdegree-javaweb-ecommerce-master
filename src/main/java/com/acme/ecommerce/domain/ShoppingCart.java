package com.acme.ecommerce.domain;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope("session")
//@Scope(proxyMode= ScopedProxyMode.TARGET_CLASS, value="session")

public class ShoppingCart {

	private Purchase purchase = null;
	private CouponCode couponCode = null;

	public Purchase getPurchase() {
		return purchase;
	}

	public void setPurchase(Purchase purchase) {
		this.purchase = purchase;
	}

	public CouponCode getCouponCode() {
		return couponCode;
	}

	public void setCouponCode(CouponCode couponCode) {
		this.couponCode = couponCode;
	}

	public boolean isEmpty(){
		return (purchase==null)||(purchase!=null && purchase.getProductPurchases().isEmpty());
	}


}
