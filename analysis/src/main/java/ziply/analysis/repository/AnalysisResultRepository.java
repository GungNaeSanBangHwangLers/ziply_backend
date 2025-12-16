package ziply.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ziply.analysis.domain.AnalysisResult;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
}