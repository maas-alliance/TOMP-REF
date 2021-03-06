package org.tomp.api.utils;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.model.Fare;
import io.swagger.model.FarePart;
import io.swagger.model.PlanningResult;
import io.swagger.model.SimpleLeg;

@Component
public class FareUtil {

	@Autowired
	LegUtil legUtil;

	public double calculateFare(PlanningResult savedOption) {
		Fare fare = null;
		double minutes = 60;
		double distanceInMeters = 1000;

		if (savedOption instanceof SimpleLeg) {
			SimpleLeg leg = ((SimpleLeg) savedOption);
			fare = leg.getPricing();
			minutes = legUtil.getDuration(leg.getLeg());
			distanceInMeters = legUtil.getDistance(leg.getLeg());
		}

		return calculateFare(fare, minutes, distanceInMeters);
	}

	public double calculateFare(Fare fare, double minutes, double distanceInMeters) {
		double amount = 0;
		BigDecimal max = null;

		for (FarePart part : fare.getParts()) {
			switch (part.getType()) {
			case FIXED:
				amount += part.getAmount().doubleValue();
				break;
			case FLEX:
				amount += calculateFlexPart(part, minutes, distanceInMeters);
				break;
			case MAX:
				max = part.getAmount();
				break;

			default:
				break;
			}
		}

		if (max != null) {
			amount = max.doubleValue();
		}

		return amount;
	}

	private double calculateFlexPart(FarePart part, double minutes, double distanceInMeters) {
		switch (part.getScaleType()) {
		case HOUR:
			double startMinutes = part.getScaleFrom().doubleValue();
			double endMinutes = part.getScaleTo().doubleValue();
			if (minutes > startMinutes && minutes < endMinutes) {
				double fareMinutes = minutes - startMinutes;
				switch (part.getUnitType()) {
				case HOUR:
					long fareHours = Math.round(fareMinutes / 60.0) + 1;
					return part.getAmount().doubleValue() * fareHours;
				case MINUTE:
					return part.getAmount().doubleValue() * fareMinutes;
				case SECOND:
					long wholeFareMinutes = Math.round(minutes);
					long fareSeconds = Math.round((minutes - wholeFareMinutes) * 60.0);
					return part.getAmount().doubleValue() * fareSeconds;
				default:
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"Illegal fare part configuration: " + part);

				}
			}
			break;
		case KM:
			break;
		case MILE:
			break;
		case MINUTE:
			break;
		default:
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown ScaleType: " + part.getScaleType());
		}
		return 0;
	}
}
