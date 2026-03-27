package ziply.analysis.dto.response;

public record TransitResult(
        int timeMin,
        String paymentStr,
        int transitCount
) {
    public static TransitResult empty() {
        return new TransitResult(0, "정보 없음", 0);
    }
}