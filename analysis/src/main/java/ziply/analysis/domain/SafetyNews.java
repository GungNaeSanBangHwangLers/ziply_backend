package ziply.analysis.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "safety_news", catalog = "ziply_analysis")
@Getter
@NoArgsConstructor
public class SafetyNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_url")
    private String contentUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "category_level", nullable = false)
    private Integer categoryLevel;   // 1, 2, 3

    @Column(name = "category_tag", length = 30)
    private String categoryTag;      // 중분류 (예: "재산 범죄", "대인 강력")

    @Column(name = "region_name")
    private String regionName;       // 예: 상도동, 동작구

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
