package ziply.review.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "base_points")
public class BasePoint {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_card_id")
    private SearchCard searchCard;

    @Column(nullable = false)
    private String alias;

    @Column(nullable = false)
    private String address;

    private Double latitude;
    private Double longitude;

    public BasePoint(String alias, String address, Double latitude, Double longitude) {
        this.alias = alias;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void assignSearchCard(SearchCard searchCard) {
        this.searchCard = searchCard;
    }
}