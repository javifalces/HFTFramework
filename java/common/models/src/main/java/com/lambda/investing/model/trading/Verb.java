package com.lambda.investing.model.trading;

public enum Verb {
	Buy, Sell, NotSet;//bid ask

	public static Verb OtherSideVerb(Verb verb) {
		if (verb == null) {
			return null;
		}
		Verb output = null;//verb is always set as original
		//check change side onlt applied on the sideActive Map
		if (verb.equals(Verb.Buy)) {
			output = Verb.Sell;
		}
		if (verb.equals(Verb.Sell)) {
			output = Verb.Buy;
		}
		return output;
	}
}


