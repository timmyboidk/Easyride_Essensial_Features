package com.easyride.location_service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@TableName("geofences")
@Data
@NoArgsConstructor
public class Geofence {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private GeofenceType type;

    @TableField("polygon_coordinates_json")
    private String polygonCoordinatesJson;

    private boolean isActive;
    private String description;

    @TableField(exist = false)
    private List<Coordinate> parsedPolygon;

    @Data
    @NoArgsConstructor
    public static class Coordinate {
        double latitude;
        double longitude;

        public Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}