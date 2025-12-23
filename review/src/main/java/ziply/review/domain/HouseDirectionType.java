package ziply.review.domain;

import lombok.Getter;

@Getter
public enum HouseDirectionType {
    SOUTH("남향"), EAST("동향"), WEST("서향"), NORTH("북향"),
    SOUTH_EAST("남동향"), SOUTH_WEST("남서향"), NORTH_WEST("북서향"), NORTH_EAST("북동향");

    private final String description;
    HouseDirectionType(String description) { this.description = description; }
}