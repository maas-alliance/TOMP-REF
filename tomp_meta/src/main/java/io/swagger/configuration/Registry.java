package io.swagger.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import io.swagger.model.Coordinates;
import io.swagger.model.MaasOperator;
import io.swagger.model.Polygon;

@Component
public class Registry {

	private Map<String, MaasOperator> map = new HashMap<>();
	private Map<String, String> tokens = new HashMap<>();
	private Map<String, Polygon> areaMap = new HashMap<>();

	public void register(MaasOperator operator) {
		map.put(operator.getId(), operator);
		tokens.put(operator.getId(), operator.getValidationToken());
		operator.setValidationToken("");
		registerArea(operator.getId(), operator.getServicedArea());
	}

	public MaasOperator get(String id) {
		return map.get(id);
	}

	public String getToken(String id) {
		return tokens.get(id);
	}

	public void registerArea(String id, Polygon serviceArea) {
		areaMap.put(id, serviceArea);
	}

	public boolean isInArea(String id, Coordinates location) {
		Polygon polygon = areaMap.get(id);
		return GeometryUtil.isInPolygon(polygon.getPoints(), location);
	}

	public List<MaasOperator> getOperators(List<Coordinates> points) {
		List<MaasOperator> locations = new ArrayList<>();

		for (Entry<String, Polygon> entry : areaMap.entrySet()) {
			if (points == null || points.isEmpty() || GeometryUtil.overlaps(points, entry.getValue().getPoints())) {
				MaasOperator maasOperator = map.get(entry.getKey());
				locations.add(maasOperator);
			}
		}

		return locations;
	}
}
