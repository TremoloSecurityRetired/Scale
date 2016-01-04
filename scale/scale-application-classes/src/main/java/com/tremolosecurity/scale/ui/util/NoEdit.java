package com.tremolosecurity.scale.ui.util;

import java.util.HashMap;

import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.ui.UiDecisions;

public class NoEdit implements UiDecisions {

	@Override
	public void init(HashMap<String, Attribute> config) {
		

	}

	@Override
	public boolean canEditUser() {
		return false;
	}

}
