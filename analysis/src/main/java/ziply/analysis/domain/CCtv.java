package ziply.analysis.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cctv")
public class CCtv {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String borough; // 자치구
    private String address; // 안심 주소
    private Integer quantity; // 수량
    private Double latitude;
    private Double longitude;

    @Builder
    public CCtv(String borough, String address, Integer quantity, Double latitude, Double longitude) {
        this.borough = borough;
        this.address = address;
        this.quantity = quantity;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}