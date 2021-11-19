package com.lambda.investing.algorithmic_trading;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter @Setter public class CustomColumn {

	String key;
	double value;

	public CustomColumn(String key, double value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public double getValue() {

		return value;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof CustomColumn))
			return false;
		CustomColumn that = (CustomColumn) o;
		return Objects.equals(key, that.key);
	}

	@Override public int hashCode() {

		return Objects.hash(key);
	}
}