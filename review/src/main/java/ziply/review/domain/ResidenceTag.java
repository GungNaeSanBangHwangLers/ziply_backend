package ziply.review.domain;

public enum ResidenceTag {
    // 내부 공간
    ROOM_SIZE("방 넓이"), STRUCTURE("구조"), STORAGE("수납공간"),

    // 생활환경
    NOISE("층간/벽간소음"), LIGHTING("채광"), BUG("벌레 유무"),
    VENTILATION("환기"), EXPENSE("교통비/관리비"),

    // 건물 & 설비
    FACILITY("설비"), PARKING("주차공간"), COMMON_AREA("공동구역"),

    // 입지 & 인프라
    PUBLIC_TRANSPORT("대중교통과의 거리"), COMMUTE_DISTANCE("직주거리"),
    INFRA("인근 상권"), SECURITY("치안/안전");

    private final String description;
    ResidenceTag(String description) { this.description = description; }
}